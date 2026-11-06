# mini-order-system

A small order management system built with **Spring Boot microservices**, inspired by my experience with IBM Sterling Order Management (OMS). The goal is to rebuild the core order flow (products, stock, orders) as independent services that talk to each other.

> **Status: work in progress.** The Inventory Service and Order Service are complete and talk to each other over REST (OpenFeign), including idempotent stock reservation, retries and a circuit breaker. Service discovery with Eureka is in place and Order finds Inventory by service name. API Gateway, security and Docker are on the roadmap below.

## Architecture

```
 Client (Postman/browser) --> [ API Gateway ]  (planned)
                                     |
               +---------------------+---------------------+
               |                                           |
    +----------v-----------+   inventory-service   +--------v-------------+
    | Inventory Service    | <-------------------- | Order Service        |
    | (8081)               |  OpenFeign + load     | (8082)               |
    +----------+-----------+  balancer, by name    +--------+-------------+
               | register / heartbeat                       | register / look up
               +---------------------+----------------------+
                                     v
                            +------------------+
                            | Eureka server    |
                            | (8761)           |
                            +------------------+
```

| Service | Port | Status |
|---|---|---|
| inventory-service | 8081 | Done (product CRUD, MySQL, validation, tests, Swagger) |
| order-service | 8082 | Done (orders, order lines, calls Inventory via OpenFeign, validation, tests) |
| api-gateway | 8080 | Planned |
| discovery-server (Eureka, service discovery) | 8761 | Done (registry and dashboard) |

Each service owns its own database (`inventory_db`, `order_db`). Services find each other through Eureka and call each other over REST. Docker Compose is planned.

## Tech stack

- Java 17+
- Spring Boot 4.x (Spring Web, Spring Data JPA, Validation, Actuator)
- MySQL 8/9 (H2 in memory for tests)
- Maven (Maven Wrapper included)
- springdoc-openapi (Swagger UI)
- Spring Cloud OpenFeign (service-to-service calls)
- JUnit 5, Mockito, MockMvc
- Resilience4j (retry and circuit breaker)
- Planned: Spring Cloud (Eureka, Gateway), Docker and Docker Compose

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
| POST | `/orders` | Place an order | 201 with a `Location` header |
| GET | `/orders/{id}` | Get an order by id | 200 (404 if not found) |
| GET | `/orders/number/{orderNumber}` | Get an order by order number | 200 (404 if not found) |
| GET | `/orders?customerEmail=...` | List a customer's orders, paged, newest first | 200 |
| POST | `/orders/{id}/cancel` | Cancel a CONFIRMED order and release its stock | 200 (404 if not found, 409 if not CONFIRMED) |
| GET | `/actuator/health` | Health check | 200 |

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

## Run locally

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
```

(In Eclipse: Run Configurations > Environment > add the variable.)

### 3. Start the services

Start the **Eureka server first** (port 8761), then Inventory (8081), then Order (8082), each in its own terminal. The Eureka dashboard at http://localhost:8761 shows both services once they have registered (up to about 40 seconds):

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

Ready-made Postman collections for both services are in `docs/postman/` (see "End-to-end flow" below).

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
```

The tests do not need MySQL or a running Inventory Service. Integration tests run with the `test` profile against an in-memory H2 database, and the Order Service uses a stub product catalog in that profile.

- Inventory Service: `ProductServiceTest` (Mockito), `ProductMapperTest`, `ProductApiTest` (MockMvc against the real controllers, validation and error handler), `ProductReservationTest` (atomic reserve and release against H2), `ReservationServiceTest` (idempotency rules), `StockApiTest` (reserve and release endpoints, all-or-nothing, repeat-safe)
- Order Service: `OrderTest`, `OrderNumberGeneratorTest`, `OrderMapperTest`, `OrderServiceTest` (Mockito, mocked stock reservation), `OrderPersistenceTest`, `OrderApiTest`, `OrderStockApiTest` (CONFIRMED and FAILED outcomes), `OrderCancelApiTest`, `InventoryProductCatalogTest` and `InventoryStockReservationTest` (Mockito, mocked Feign client), `InventoryClientTest` (the client is found by service name, not a fixed URL), `ResilienceIntegrationTest` (retry and circuit breaker in a real Spring context with a mocked Feign client). The Eureka client is switched off in the test profiles, so no test needs a running registry

## End-to-end flow (Postman)

Two collections live in `docs/postman/`:

| File | Purpose |
|---|---|
| `mini-order-system.postman_collection.json` | Every request for both services, for manual exploration |
| `e2e-flow.postman_collection.json` | An automated end-to-end flow with checks on every request (28 requests) |

Start MySQL, then Inventory (8081) and Order (8082), then in Postman choose **Import** and select `e2e-flow.postman_collection.json`, open the collection and press **Run**. Or from a terminal (needs Node.js):

```
npx newman run docs/postman/e2e-flow.postman_collection.json
```

Each run creates its own test product and customer, so it can be repeated, and it deletes the product at the end. The base URLs are collection variables (`inventoryUrl`, `orderUrl`), so only those two values change when an API Gateway is added.

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
- [ ] API Gateway
- [ ] Security
- [ ] Docker and Docker Compose

## Author

Abhijit Soudi, Technology Analyst with experience in IBM Sterling OMS and Java, building skills in Spring Boot and microservices.
