# mini-order-system

A small order management system built with **Spring Boot microservices**, inspired by my experience with IBM Sterling Order Management (OMS). The goal is to rebuild the core order flow (products, stock, orders) as independent services that talk to each other.

> **Status: work in progress.** The Inventory Service and Order Service are complete and talk to each other over REST (OpenFeign). Service discovery, API Gateway, stock reservation, resilience and security are on the roadmap below.

## Architecture

```
                    +-----------------+
   Client  -------> |   API Gateway   |   (planned)
 (Postman/browser)  +--------+--------+
                             |
              +--------------+--------------+
              |                             |
     +--------v--------+           +--------v--------+
     | Inventory       | <-------- | Order           |
     | Service (8081)  |  GET /products/sku/{sku}    |
     +-----------------+  (OpenFeign) +--------------+
                                    | Service (8082)  |
                                    +-----------------+
```

| Service | Port | Status |
|---|---|---|
| inventory-service | 8081 | Done (product CRUD, MySQL, validation, tests, Swagger) |
| order-service | 8082 | Done (orders, order lines, calls Inventory via OpenFeign, validation, tests) |
| api-gateway | 8080 | Planned |
| Eureka server (service discovery) | 8761 | Planned |

Each service owns its own database (`inventory_db`, `order_db`). Services call each other over REST and will be started together with Docker Compose.

## Tech stack

- Java 17+
- Spring Boot 4.x (Spring Web, Spring Data JPA, Validation, Actuator)
- MySQL 8/9 (H2 in memory for tests)
- Maven (Maven Wrapper included)
- springdoc-openapi (Swagger UI)
- Spring Cloud OpenFeign (service-to-service calls)
- JUnit 5, Mockito, MockMvc
- Planned: Spring Cloud (Eureka, Gateway, Resilience4j), Docker and Docker Compose

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
{ "id": 6, "sku": "SKU-1006", "name": "Pen", "category": "Stationery", "price": 20.00, "quantity": 500 }
```

`GET /products` returns a page:

```json
{
  "content": [ { "id": 1, "sku": "SKU-1001", "name": "Laptop", "category": "Electronics", "price": 55000.00, "quantity": 10 } ],
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

## Order Service

Creates and reads orders. When an order is placed, the service calls the Inventory Service for each SKU (name, price, stock) and stores a **snapshot** of the name and unit price on each order line, so later price changes never rewrite old orders. The service has its own database (`order_db`).

### API

| Method | URL | Description | Success status |
|---|---|---|---|
| POST | `/orders` | Place an order | 201 with a `Location` header |
| GET | `/orders/{id}` | Get an order by id | 200 (404 if not found) |
| GET | `/orders/number/{orderNumber}` | Get an order by order number | 200 (404 if not found) |
| GET | `/orders?customerEmail=...` | List a customer's orders, paged, newest first | 200 |
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

`InventoryClient` (OpenFeign) calls `GET {inventory.base-url}/products/sku/{sku}`. The base URL is set in `application.properties` (`http://localhost:8081`). Timeouts: 2 s connect, 5 s read.

| Situation | Result |
|---|---|
| Invalid request body | 400 with field details |
| SKU not found in Inventory | 400 |
| Quantity is more than stock | 409 Conflict, no order is saved |
| Inventory stopped or returns 5xx | 503 Service Unavailable |
| Everything fine | 201, status `CREATED` |

Errors use the same JSON shape as the Inventory Service, with an added `path` field.

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

Start **Inventory first** (port 8081), then Order (port 8082), each in its own terminal:

```
git clone https://github.com/abhijitsoudi1997/mini-order-system.git
cd mini-order-system/inventory-service
./mvnw spring-boot:run
```
```
cd mini-order-system/order-service
./mvnw spring-boot:run
```

A ready-made Postman collection for both services is in `docs/postman/`.

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

- Inventory Service (17): `ProductServiceTest` (Mockito), `ProductMapperTest`, `ProductApiTest` (MockMvc against the real controllers, validation and error handler)
- Order Service (26): `OrderTest`, `OrderNumberGeneratorTest`, `OrderMapperTest`, `OrderServiceTest`, `OrderPersistenceTest`, `OrderApiTest`, `OrderStockApiTest` (stock and rollback rules), `InventoryProductCatalogTest` (Mockito, mocked Feign client)

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
- [ ] Stock reservation and consistency between services
- [ ] Resilience4j (retry, circuit breaker)
- [ ] Service discovery (Eureka), API Gateway
- [ ] Security
- [ ] Docker and Docker Compose

## Author

Abhijit Soudi, Technology Analyst with experience in IBM Sterling OMS and Java, building skills in Spring Boot and microservices.
