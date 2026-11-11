# mini-order-system

A small order management system built with **Spring Boot microservices**, inspired by my experience with IBM Sterling Order Management (OMS). The goal is to rebuild the core order flow (products, stock, orders) as independent services that talk to each other.

> **Status: work in progress.** The Inventory Service and Order Service are complete and talk to each other over REST (OpenFeign), including idempotent stock reservation, retries and a circuit breaker. Service discovery with Eureka is in place and Order finds Inventory by service name. An API Gateway is the single entry point for clients, and the Order Service is protected with JWT login and roles. Every service has a Docker image, and Docker Compose starts the whole system (MySQL included) with one command.

## Architecture

```
 Client (Postman/browser)
          |  http://localhost:8080/api/...
          v
 +----------------------+
 | API Gateway (8080)   |  routes by service name (lb://...)
 +-----+-----------+----+
       |           |
       | /api/products/**         /api/orders/**
       v                               v
    +----------------------+   inventory-service   +----------------------+
    | Inventory Service    | <-------------------- | Order Service        |
    | (8081)               |  OpenFeign + load     | (8082)               |
    +----------+-----------+  balancer, by name    +--------+-------------+
               | register / heartbeat                       | register / look up
               +---------------------+----------------------+
                                     v
                            +------------------+
                            | Eureka server    |  (the gateway registers and
                            | (8761)           |   looks up services here too)
                            +------------------+
```

| Service | Port | Status |
|---|---|---|
| inventory-service | 8081 | Done (product CRUD, MySQL, validation, tests, Swagger) |
| order-service | 8082 | Done (orders, order lines, calls Inventory via OpenFeign, JWT security with USER and ADMIN roles, validation, tests) |
| api-gateway | 8080 | Done (routes by service name, Location rewrite, correlation id, timeouts, tests) |
| discovery-server (Eureka, service discovery) | 8761 | Done (registry and dashboard) |

Each service owns its own database (`inventory_db`, `order_db`). Services find each other through Eureka and call each other over REST. Clients use the gateway. Docker Compose runs the whole system.

## Tech stack

- Java 17+
- Spring Boot 4.x (Spring Web, Spring Data JPA, Validation, Actuator)
- MySQL 8/9 (H2 in memory for tests)
- Maven (Maven Wrapper included)
- springdoc-openapi (Swagger UI)
- Spring Cloud OpenFeign (service-to-service calls)
- JUnit 5, Mockito, MockMvc
- Resilience4j (retry and circuit breaker)
- Spring Cloud Netflix Eureka (service discovery) and Spring Cloud Gateway (reactive, WebFlux)
- Spring Security with JWT (OAuth2 resource server, HS256)
- Docker (multi-stage images, non-root user) and Docker Compose

## Inventory Service

Manages the product catalogue. Products are stored in MySQL. Deleting a product is a **soft delete**: the row stays with `active = false` and is hidden from the API.

### API

| Method | URL | Description | Success status |
|---|---|---|---|
| GET | `/products` | List active products, paged (`page`, `size`, `sort`) | 200 |
| GET | `/products?category=Electronics` | List products in a category | 200 |
| GET | `/products/{id}` | Get one product by id | 200 (404 if not found) |
| GET | `/products/sku/{sku}` | Get one product by SKU | 200 (404 if not found) |
| POST | `/products` | Create a product | 201 with a `Location` header (400 invalid, 409 duplicate SKU) |
| PUT | `/products/{id}` | Replace a product | 200 (400 invalid, 404 if not found) |
| DELETE | `/products/{id}` | Soft delete a product | 204 (404 if not found) |
| POST | `/inventory/reserve` | Reserve stock for an order (all items or none, safe to repeat for the same order number) | 200 (400 invalid, 404 unknown SKU, 409 not enough stock, order number reused with different items, or already released) |
| POST | `/inventory/release` | Release everything reserved for an order number (safe to repeat) | 200 (400 invalid, 404 no reservation for that order) |
| GET | `/hello` | Returns the configured welcome message | 200 |
| GET | `/actuator/health` | Health check | 200 |

Interactive documentation: **http://localhost:8081/swagger-ui/index.html** (raw OpenAPI JSON at `/v3/api-docs`).

### Request and response JSON

Create or update a product (no `id`, it is generated):

```json
{
  "sku": "SKU-1006",
  "name": "Pen",
  "category": "Stationery",
  "price": 20.00,
  "quantity": 500
}
```

Validation rules: `sku`, `name` and `category` must not be blank, `price` is required and greater than 0, `quantity` is 0 or more.

A product in a response also has its `id`:

```json
{ "id": 6, "sku": "SKU-1006", "name": "Pen", "category": "Stationery", "price": 20.00, "quantity": 500, "reservedQuantity": 0, "availableQuantity": 500 }
```

`GET /products` returns a page:

```json
{
  "content": [ { "id": 1, "sku": "SKU-1001", "name": "Laptop", "category": "Electronics", "price": 55000.00, "quantity": 10, "reservedQuantity": 0, "availableQuantity": 10 } ],
  "page": { "size": 10, "number": 0, "totalElements": 5, "totalPages": 1 }
}
```

### Error format

Every error has the same JSON shape (404, 400 validation, 409 duplicate SKU, 500):

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": ["name: name is required", "price: price must be greater than 0"],
  "timestamp": "2026-10-30T17:45:12.345"
}
```

### Example requests

```
# list the first 5 products, cheapest last
curl "http://localhost:8081/products?size=5&sort=price,desc"

# get by SKU
curl http://localhost:8081/products/sku/SKU-1001

# create a product
curl -X POST http://localhost:8081/products \
  -H "Content-Type: application/json" \
  -d '{"sku":"SKU-1006","name":"Pen","category":"Stationery","price":20.00,"quantity":500}'

# soft delete
curl -X DELETE http://localhost:8081/products/6
```

### Stock reservation

`quantity` is the total on hand. `reservedQuantity` is held for orders, and `availableQuantity = quantity - reservedQuantity`. Reserve and release take the same body:

```json
{ "orderNumber": "ORD-20261103-AB12CD", "items": [ { "sku": "SKU-1001", "quantity": 2 } ] }
```

Reserving is **atomic and all-or-nothing**. Each item is one conditional SQL `UPDATE ... WHERE quantity - reserved_quantity >= :qty`, so two orders competing for the last unit can never oversell. If any item cannot be reserved, the whole call is rolled back.

Reservations are **idempotent per order number**. Each successful reserve stores a reservation record (`stock_reservation` and `stock_reservation_line` tables). Sending the same reserve again does nothing (the stock is already held), and releasing twice is harmless. This is what makes it safe for the Order Service to retry after a lost reply. Reusing an order number with different items, or after it was released, returns 409. Release gives back exactly what was stored for that order.

## Order Service

Creates and reads orders. When an order is placed, the service calls the Inventory Service for each SKU (name, price, stock) and stores a **snapshot** of the name and unit price on each order line, so later price changes never rewrite old orders. The service has its own database (`order_db`).

### API

| Method | URL | Description | Success status |
|---|---|---|---|
| POST | `/auth/login` | Log in with a username and password, returns a JWT (no token needed) | 200 (401 wrong credentials, 400 blank fields) |
| POST | `/orders` | Place an order (needs a token) | 201 with a `Location` header |
| GET | `/orders/{id}` | Get an order by id | 200 (404 if not found) |
| GET | `/orders/number/{orderNumber}` | Get an order by order number | 200 (404 if not found) |
| GET | `/orders?customerEmail=...` | List a customer's orders, paged, newest first | 200 |
| POST | `/orders/{id}/cancel` | Cancel a CONFIRMED order and release its stock | 200 (404 if not found, 409 if not CONFIRMED) |
| GET | `/actuator/health` | Health check (no token needed) | 200 |

Every `/orders` call needs `Authorization: Bearer <token>`: no or an invalid token gives 401, and cancelling as a non-admin gives 403. See "Security" below.

### Place an order

```json
{
  "customerName": "Abhijit Soudi",
  "customerEmail": "abhijit@example.com",
  "customerPhone": "9876543210",
  "shippingAddress": { "line": "12 MG Road", "city": "Mysore", "state": "Karnataka", "pincode": "570001" },
  "items": [ { "sku": "SKU-1001", "quantity": 1 }, { "sku": "SKU-1002", "quantity": 2 } ]
}
```

### How Order talks to Inventory

`InventoryClient` (OpenFeign) calls `GET /products/sku/{sku}` for the name and price, then `POST /inventory/reserve` to hold the stock (and `POST /inventory/release` when an order is cancelled). Inventory is found by its **Eureka service name** `inventory-service` (there is no fixed URL), and Spring Cloud LoadBalancer picks one of the registered instances. Timeouts: 2 s connect, 5 s read.

| Situation | Result |
|---|---|
| Invalid request body | 400 with field details |
| SKU not found in Inventory | 400 |
| Not enough stock | 409 Conflict, the order is saved with status `FAILED` and a `failureReason` |
| Inventory stopped or returns 5xx | 503 Service Unavailable |
| Stock reserved | 201, status `CONFIRMED` |
| Cancelling an order that is not CONFIRMED | 409 |
| No token, an expired, tampered or wrongly signed token | 401 |
| A valid token without the needed role (a customer cancelling) | 403 |

Errors use the same JSON shape as the Inventory Service, with an added `path` field.

### Order lifecycle

```
CREATED --reserve ok--> CONFIRMED --cancel (stock released)--> CANCELLED
   |
   +--not enough stock--> FAILED (kept, with failureReason)
```

If Inventory is unreachable, the request fails with 503 and nothing is saved.

### Resilience (Resilience4j)

All calls from Order to Inventory share one circuit breaker named `inventory`.

| Call | Retry | Circuit breaker |
|---|---|---|
| Product lookup (`GET /products/sku/{sku}`) | up to 3 attempts, 500 ms apart | yes |
| Reserve (`POST /inventory/reserve`) | up to 3 attempts (safe: reserve is idempotent per order number) | yes |
| Release (`POST /inventory/release`) | up to 3 attempts (safe: release is idempotent) | yes |

Only "Inventory unreachable or 5xx" is retried and counted as a failure. Business answers (404 unknown SKU, 409 not enough stock) are final and never retried.

Circuit breaker: it looks at the last 10 calls, opens when at least 5 calls were made and half of them failed, stays open for 10 s, then lets 2 trial calls through. While it is open, requests fail immediately with 503. If every retry of a reserve fails, Order makes a best-effort release in case Inventory reserved before the reply was lost.

State is visible at `/actuator/circuitbreakers`, `/actuator/retries` and `/actuator/health`. The settings live under `resilience4j.*` in `order-service/src/main/resources/application.properties`.

## Service discovery (Eureka)

The `discovery-server` project is a Eureka server (port 8761, dashboard at http://localhost:8761). Inventory and Order register with it at start-up under their `spring.application.name` and then send heartbeats. Order does not know where Inventory runs: it asks for `inventory-service`, and Spring Cloud LoadBalancer picks one of the registered instances (round robin). Several copies of Inventory can therefore run on different ports (for example with `-Dserver.port=8091`) and share the same database.

The timers are shortened for local work so that changes show up within seconds. A real deployment would keep the defaults or tune them deliberately.

| Setting | Local | Default |
|---|---|---|
| Heartbeat from each service | every 5 s | every 30 s |
| Instance expires after missed heartbeats | 15 s | 90 s |
| Server checks for expired instances | every 5 s | every 60 s |
| Self-preservation mode (server) | off | on |
| Order refreshes its copy of the registry | every 5 s | every 30 s |
| LoadBalancer cache in Order | 5 s | 35 s |

Failure behaviour:

- **One of two Inventory instances crashes:** until it expires from the registry, Order may pick it and the call fails. The retry then goes to the other instance, so the order usually still succeeds.
- **All Inventory instances are gone:** the load balancer has nobody to call, Order answers 503 and the circuit breaker opens after repeated failures. When Inventory registers again, orders work again.
- **The Eureka server is stopped:** running services keep working, because each client keeps a local copy of the registry. They re-register when the server returns.
- **A clean shutdown** deregisters the instance at once, while a hard kill leaves it in the registry until its lease expires.

## API Gateway

The `api-gateway` project (Spring Cloud Gateway, port 8080) is the single entry point. It registers with Eureka and forwards requests to the services **by service name** (`lb://inventory-service`, `lb://order-service`), so it does not know any service port.

| Public URL | Forwarded to |
|---|---|
| `http://localhost:8080/api/products/**` | inventory-service `/products/**` |
| `http://localhost:8080/api/orders/**` | order-service `/orders/**` |
| `http://localhost:8080/api/auth/login` | order-service `/auth/login` (only this one path of `/auth`) |

The `/api` prefix is removed by the gateway (`StripPrefix=1`), so the services keep their own paths.

- **Only the public endpoints are routed.** `/inventory/reserve`, `/inventory/release` and the actuator endpoints of the services are internal. The login route is limited to the single path `/api/auth/login`. They are not reachable through the gateway (404). Routes are listed explicitly, not generated automatically from the registry, so a new service is never exposed by accident.
- **`Location` header:** a created resource is returned with a public path such as `/api/products/12`, not the internal `http://localhost:8081/...` address (`RewriteResponseHeader` on both routes).
- **Correlation id:** a global filter adds an `X-Correlation-Id` to every request. A valid id sent by the client is kept, otherwise a UUID is generated. The id is forwarded to the services, returned in the response and logged by the gateway.
- **Timeouts:** 2 s to connect, 10 s for a response. A service that does not answer in time gives the client a 504, a stopped service gives 503, and a path with no route gives 404.

Routes are defined in `api-gateway/src/main/resources/application.properties` under `spring.cloud.gateway.server.webflux.routes`. The routes can be inspected at `http://localhost:8080/actuator/gateway/routes`.

The gateway passes the `Authorization` header on to the services unchanged. It does not check tokens itself: the Order Service does (see "Security").

## Security

The Order Service is protected with **JWT bearer tokens** (Spring Security, OAuth2 resource server).

How it works:

1. `POST /auth/login` with `{"username": "...", "password": "..."}` checks the credentials and returns a signed token (`accessToken`, `tokenType: Bearer`, `expiresIn: 1800`).
2. The client sends `Authorization: Bearer <token>` on every other request.
3. The Order Service verifies the signature (HS256) and the expiry on each request. It keeps no session (stateless).

| Role | Login | Allowed |
|---|---|---|
| `USER` | `customer` | place, read and list orders |
| `ADMIN` | `admin` | the same, plus cancel an order |
| none | | `/actuator/health`, `/actuator/info` and `/auth/login` only |

| Situation | Status |
|---|---|
| No token, expired token, tampered token, token signed with another key, Basic auth | 401 |
| Valid token but not allowed (customer cancelling, token without roles) | 403 |
| Wrong password or unknown user at login | 401, the same message for both |

```
# log in, then use the token
curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"username":"customer","password":"..."}'
curl -H "Authorization: Bearer <accessToken>" "http://localhost:8080/api/orders?customerEmail=abhijit@example.com"
```

Design notes and limits:

- The token is **signed, not encrypted**: it holds the username, roles, issuer and times, never secrets. It cannot be revoked before it expires, so it is short-lived (30 minutes, `app.security.jwt.expiry-minutes`).
- Passwords are stored as BCrypt hashes. The signing secret and both passwords come from environment variables (`JWT_SECRET` must be at least 32 bytes, otherwise the service refuses to start).
- The two users live in memory. A real system would use a database or an identity provider (for example Keycloak) that issues the tokens, with the services only validating them.
- The Inventory Service has no authentication yet. It is meant to be reachable only inside the private network (Docker comes next). Until then, port 8081 is open on a developer machine.
- CSRF protection is off because there are no cookies or browser sessions.

## Docker

Each service (`discovery-server`, `inventory-service`, `order-service`, `api-gateway`) has a **multi-stage `Dockerfile`** and a `.dockerignore` in its folder.

- **Stage 1 (build):** `eclipse-temurin:17-jdk` runs the Maven wrapper and builds the jar (tests are skipped in the image build and run separately with `./mvnw test`). Maven's download cache is reused between builds.
- **Stage 2 (run):** `eclipse-temurin:17-jre` receives only the jar. The image contains no source code, no Maven and no compiler, and the application runs as a normal user, not as root.

Build all four images (needs only Docker, not Java or Maven):

```
cd discovery-server  && docker build -t discovery-server:1.0 .  && cd ..
cd inventory-service && docker build -t inventory-service:1.0 . && cd ..
cd order-service     && docker build -t order-service:1.0 .     && cd ..
cd api-gateway       && docker build -t api-gateway:1.0 .       && cd ..
```

### Configuration with environment variables

Nothing secret or environment-specific is inside an image. The same image runs anywhere, and settings are passed when the container starts. Spring Boot turns an environment variable into a property (uppercase, dots and dashes become underscores) and it overrides the value in `application.properties`.

| Variable | Used by | Purpose |
|---|---|---|
| `SPRING_DATASOURCE_URL` | inventory, order | JDBC URL, for example `jdbc:mysql://mysql:3306/inventory_db` |
| `DB_PASSWORD` / `ORDER_DB_PASSWORD` | inventory / order | database passwords |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | inventory, order, gateway | Eureka address, for example `http://eureka:8761/eureka/` |
| `EUREKA_INSTANCE_HOSTNAME` | inventory, order, gateway | the name the service registers under (its container name) |
| `ORDER_USER_PASSWORD`, `ORDER_ADMIN_PASSWORD`, `JWT_SECRET` | order | login passwords and token signing key |

Inside a container, `localhost` means the container itself. Containers therefore reach each other **by container name on a shared Docker network** (for example `mysql`, `eureka`, `inventory-service`), and the local value `eureka.instance.hostname=localhost` in the properties files is replaced through `EUREKA_INSTANCE_HOSTNAME`. Docker Compose (next section) starts the whole system in one command.

### Docker Compose

`docker-compose.yml` in the project root describes the whole system: MySQL, Eureka, Inventory, Order and the gateway. Compose creates the network and the volume, builds the images, and starts everything in the right order.

Files:

| File | Purpose |
|---|---|
| `docker-compose.yml` | the five services |
| `.env.example` | template for the secrets file. Copy it to `.env` and fill in the values. **`.env` is ignored by Git and must never be committed** |
| `docker/mysql-init/01-create-databases.sh` | creates `inventory_db`, `order_db` and their users the first time the MySQL volume is created |
| `docker/healthcheck.sh` | small script used by the health checks (the images have no `curl`) |
| `docker-compose.scale.yml` | demo override for running several Inventory instances |

Start, check and stop:

```
cp .env.example .env        # then edit .env and set real values (JWT_SECRET: openssl rand -hex 32)
docker compose up -d --build
docker compose ps           # wait until every service is (healthy)
docker compose logs -f order-service
docker compose down         # stops everything, keeps the database volume
docker compose down -v      # also deletes the database volume (fresh start)
```

| Service | Port on the Mac | Notes |
|---|---|---|
| api-gateway | 8080 | the entry point for clients |
| eureka | 8761 | dashboard |
| inventory-service | 8081 | also reachable directly, for the Postman flow |
| order-service | 8082 | also reachable directly |
| mysql | 3307 | for MySQL Workbench (3307 avoids a clash with a local MySQL) |

How it fits together:

- **Start order:** MySQL and Eureka must be `healthy` before Inventory and Order start (`depends_on` with `condition: service_healthy`). Health checks call `/actuator/health` (a 200 means UP).
- **Names, not IPs:** services reach each other by service name on the Compose network (`mysql`, `eureka`). Each registers in Eureka under its own service name (`EUREKA_INSTANCE_HOSTNAME`).
- **Secrets:** all passwords and the JWT key come from `.env`. A missing value stops Compose immediately with a clear message.
- **Memory:** each container has a limit (`mem_limit`) and Java is told to use half of it for the heap (`-XX:MaxRAMPercentage=50`). Check with `docker stats`.
- **Restart:** a service that crashes is restarted (`restart: on-failure`). One that is stopped on purpose stays stopped.
- **Database setup runs only once.** The init script runs when the MySQL volume is empty. Changing it, or a password in `.env`, has no effect on an existing volume: use `docker compose down -v` to start fresh (this deletes the data).
- **Two Inventory instances (load balancing demo):**
  `docker compose -f docker-compose.yml -f docker-compose.scale.yml up -d --scale inventory-service=2`
  Eureka then lists two instances and the gateway alternates between them. Return to one with `docker compose up -d`.

## Run locally

**Easiest:** use Docker Compose (see "Docker Compose" above). It needs only Docker. The rest of this section runs the services directly with Java, which is useful when working on the code in an IDE.

Requirements: Java 17 or newer and MySQL. Maven is not needed because each service includes the Maven Wrapper.

### 1. Create the databases and users

```sql
CREATE DATABASE inventory_db;
CREATE USER 'inventory_user'@'localhost' IDENTIFIED BY 'choose-a-password';
GRANT ALL PRIVILEGES ON inventory_db.* TO 'inventory_user'@'localhost';

CREATE DATABASE order_db;
CREATE USER 'order_user'@'localhost' IDENTIFIED BY 'choose-another-password';
GRANT ALL PRIVILEGES ON order_db.* TO 'order_user'@'localhost';
```

Hibernate creates the tables on first start (`spring.jpa.hibernate.ddl-auto=update`) and the Inventory Service seeds 5 sample products when its table is empty.

### 2. Set the passwords as environment variables

Passwords are not stored in the repository. The services read them from environment variables:

```
export DB_PASSWORD='choose-a-password'              # inventory-service
export ORDER_DB_PASSWORD='choose-another-password'  # order-service
export ORDER_USER_PASSWORD='choose-a-user-login-password'    # order-service login: customer
export ORDER_ADMIN_PASSWORD='choose-an-admin-login-password' # order-service login: admin
export JWT_SECRET="$(openssl rand -hex 32)"                  # order-service token signing key (at least 32 characters)
```

(In Eclipse: Run Configurations > Environment > add the variable.)

### 3. Start the services

Start the **Eureka server first** (port 8761), then Inventory (8081), Order (8082) and the API Gateway (8080), each in its own terminal. The Eureka dashboard at http://localhost:8761 shows the services once they have registered (about 10 seconds with the local timers; wait before the first request):

```
git clone https://github.com/abhijitsoudi1997/mini-order-system.git
cd mini-order-system/discovery-server
./mvnw spring-boot:run
```
```
cd mini-order-system/inventory-service
./mvnw spring-boot:run
```
```
cd mini-order-system/order-service
./mvnw spring-boot:run
```
```
cd mini-order-system/api-gateway
./mvnw spring-boot:run
```

Then use the gateway, for example `curl http://localhost:8080/api/products/sku/SKU-1001`.

Ready-made Postman collections are in `docs/postman/` (see "End-to-end flow" below). For the Order Service, log in first (`POST /auth/login`) and send the token.

### Profiles

Configuration is split by environment:

- `application.properties`: shared settings (datasource, JPA, port)
- `application-dev.properties`: development settings (more logging, health details shown)
- `application-prod.properties`: production-style settings
- `application-test.properties`: used by tests only (H2 database, stub product catalog)

Select one with `spring.profiles.active` in `application.properties`, or at run time:

```
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Tests

```
cd inventory-service && ./mvnw test
cd order-service && ./mvnw test
cd api-gateway && ./mvnw test
```

The tests do not need MySQL, Eureka or a running service. Integration tests run with the `test` profile against an in-memory H2 database, and the Order Service uses a stub product catalog in that profile.

- Inventory Service: `ProductServiceTest` (Mockito), `ProductMapperTest`, `ProductApiTest` (MockMvc against the real controllers, validation and error handler), `ProductReservationTest` (atomic reserve and release against H2), `ReservationServiceTest` (idempotency rules), `StockApiTest` (reserve and release endpoints, all-or-nothing, repeat-safe)
- Order Service: `OrderTest`, `OrderNumberGeneratorTest`, `OrderMapperTest`, `OrderServiceTest` (Mockito, mocked stock reservation), `OrderPersistenceTest`, `OrderApiTest`, `OrderStockApiTest` (CONFIRMED and FAILED outcomes), `OrderCancelApiTest`, `InventoryProductCatalogTest` and `InventoryStockReservationTest` (Mockito, mocked Feign client), `InventoryClientTest` (the client is found by service name, not a fixed URL), `ResilienceIntegrationTest` (retry and circuit breaker in a real Spring context with a mocked Feign client), `TokenServiceTest` (token contents and expiry, short secrets rejected) and `SecurityApiTest` (login, 401 for missing, expired, tampered, foreign-key and Basic credentials, 403 for a customer cancelling or a token without roles, admin allowed). The Eureka client is switched off in the test profiles, so no test needs a running registry
- API Gateway: `CorrelationIdFilterTest` (id generated, kept, replaced when invalid or too long, runs first) and `RoutesTest` (exactly three routes, each goes to the right service name, only `/api/auth/login` is open under `/auth`, internal and unprefixed paths match no route)

## End-to-end flow (Postman)

Two collections live in `docs/postman/`:

| File | Purpose |
|---|---|
| `mini-order-system.postman_collection.json` | Every request for both services, for manual exploration |
| `e2e-flow.postman_collection.json` | An automated end-to-end flow directly against the services, with checks on every request (33 requests) |
| `e2e-gateway.postman_collection.json` | The same flow through the API gateway (35 requests): public calls use `http://localhost:8080/api/...`, and it also checks that internal endpoints are not exposed that `Location` is a public path, and the same security checks |

Start the system (`docker compose up -d`, or MySQL, Eureka, Inventory (8081), Order (8082) and the gateway (8080) by hand), then in Postman choose **Import** and select `e2e-flow.postman_collection.json`. The collections log in first, so open the collection's **Variables** tab and type the two login passwords (the values you set as `ORDER_USER_PASSWORD` and `ORDER_ADMIN_PASSWORD`) into `customerPassword` and `adminPassword` as **Current value** (they are empty in the file and must never be committed). Then press **Run**. Or from a terminal (needs Node.js):

```
npx newman run docs/postman/e2e-flow.postman_collection.json --env-var "customerPassword=..." --env-var "adminPassword=..."
npx newman run docs/postman/e2e-gateway.postman_collection.json --env-var "customerPassword=..." --env-var "adminPassword=..."
```

Each run creates its own test product and customer, so it can be repeated, and it deletes the product at the end. The base URLs are collection variables (`inventoryUrl`, `orderUrl`, and `gatewayUrl` in the gateway collection). Health checks and the internal reserve and release calls always go straight to the services.

What the flow checks:

1. Both services are healthy.
2. A product is created with 5 in stock and nothing reserved.
3. An order for 2 units is **CONFIRMED**, and Inventory now shows 2 reserved and 3 available.
4. An order for 4 units (only 3 available) is saved as **FAILED** with a failure reason and returns 409. It reserves nothing.
5. The orders can be read by id, by order number and by customer email.
6. An unknown SKU and an invalid body both return 400.
7. A price change in Inventory does not change the total of an existing order (price snapshot).
8. Cancelling the confirmed order gives **CANCELLED** and releases the stock. Cancelling again, cancelling a failed order and cancelling an unknown order return 409, 409 and 404.
9. Reserve and release in Inventory are idempotent: sending the same request twice changes the stock once, and it never goes below zero.
10. The test product is deleted and then returns 404.
11. Security: both logins return a Bearer token, an order list without a token returns 401, a customer token cannot cancel (403), and a tampered token returns 401. Cancelling uses the admin token.

Failure behaviour (stopping or freezing Inventory, the circuit breaker opening and closing) needs a service to be stopped, so it is checked by hand. See the Resilience section above and `/actuator/circuitbreakers`.

## Roadmap

- [x] Inventory Service: product CRUD
- [x] Profiles, Actuator and logging
- [x] Spring Data JPA with H2, then MySQL
- [x] DTOs, mapper and layered architecture
- [x] Validation and global exception handling
- [x] Unit and integration tests
- [x] Swagger / OpenAPI
- [x] Pagination, get by SKU, soft delete, BigDecimal prices
- [x] Order Service with orders and order lines
- [x] Service-to-service call (OpenFeign), stock check, error handling
- [x] Stock reservation (reserve, release, confirm, cancel)
- [x] Resilience4j (retry, circuit breaker) and idempotent reserve and release
- [x] End-to-end flow documented and automated (Postman collection with tests)
- [x] Service discovery (Eureka) and lookup by service name
- [x] API Gateway (routes by service name, Location rewrite, correlation id, timeouts)
- [x] Security (JWT login, roles USER and ADMIN, 401 and 403)
- [x] Docker images (multi-stage, non-root, configured with environment variables)
- [x] Docker Compose (one command for MySQL, Eureka, Inventory, Order and the gateway; health checks, memory limits)

## Author

Abhijit Soudi, Technology Analyst with experience in IBM Sterling OMS and Java, building skills in Spring Boot and microservices.
