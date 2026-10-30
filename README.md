# mini-order-system

A small order management system built with **Spring Boot microservices**, inspired by my experience with IBM Sterling Order Management (OMS). The goal is to rebuild the core order flow (products, stock, orders) as independent services that talk to each other.

> **Status: work in progress.** The Inventory Service is complete (REST API, MySQL, validation, error handling, tests, Swagger). The Order Service, API Gateway and the rest are on the roadmap below.

## Architecture (planned)

```
                    +-----------------+
   Client  -------> |   API Gateway   |
 (Postman/browser)  +--------+--------+
                             |
              +--------------+--------------+
              |                             |
     +--------v--------+           +--------v--------+
     | Inventory       | <-------- | Order           |
     | Service (8081)  |  stock    | Service (8082)  |
     +-----------------+  check    +-----------------+
```

| Service | Port | Status |
|---|---|---|
| inventory-service | 8081 | Done (product CRUD, MySQL, validation, tests, Swagger) |
| order-service | 8082 | Planned |
| api-gateway | 8080 | Planned |
| Eureka server (service discovery) | 8761 | Planned |

Each service will own its database. Services will call each other over REST and be started together with Docker Compose.

## Tech stack

- Java 17+
- Spring Boot 4.x (Spring Web, Spring Data JPA, Validation, Actuator)
- MySQL 8/9 (H2 in memory for tests)
- Maven (Maven Wrapper included)
- springdoc-openapi (Swagger UI)
- JUnit 5, Mockito, MockMvc
- Planned: Spring Cloud (Eureka, Gateway, OpenFeign, Resilience4j), Docker and Docker Compose

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

## Run locally

Requirements: Java 17 or newer and MySQL. Maven is not needed because the project includes the Maven Wrapper.

### 1. Create the database and user

```sql
CREATE DATABASE inventory_db;
CREATE USER 'inventory_user'@'localhost' IDENTIFIED BY 'choose-a-password';
GRANT ALL PRIVILEGES ON inventory_db.* TO 'inventory_user'@'localhost';
```

Hibernate creates the `product` table on first start (`spring.jpa.hibernate.ddl-auto=update`) and the app seeds 5 sample products when the table is empty.

### 2. Set the password as an environment variable

The password is not stored in the repository. The app reads it from `DB_PASSWORD`:

```
export DB_PASSWORD='choose-a-password'
```

(In Eclipse: Run Configurations > Environment > add `DB_PASSWORD`.)

### 3. Start the service

```
git clone https://github.com/abhijitsoudi1997/mini-order-system.git
cd mini-order-system/inventory-service
./mvnw spring-boot:run
```

The service starts on `http://localhost:8081`.

### Profiles

Configuration is split by environment:

- `application.properties`: shared settings (datasource, JPA, port)
- `application-dev.properties`: development settings (more logging, health details shown)
- `application-prod.properties`: production-style settings

Select one with `spring.profiles.active` in `application.properties`, or at run time:

```
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Tests

```
cd inventory-service
./mvnw test
```

The tests do not need MySQL. Integration tests run with the `test` profile against an in-memory H2 database.

- Unit tests: `ProductServiceTest` (Mockito), `ProductMapperTest`
- Integration tests: `ProductApiTest` (MockMvc against the real controllers, validation and error handler)

## Roadmap

- [x] Inventory Service: product CRUD
- [x] Profiles, Actuator and logging
- [x] Spring Data JPA with H2, then MySQL
- [x] DTOs, mapper and layered architecture
- [x] Validation and global exception handling
- [x] Unit and integration tests
- [x] Swagger / OpenAPI
- [x] Pagination, get by SKU, soft delete, BigDecimal prices
- [ ] Order Service with orders and order lines
- [ ] Service-to-service calls (Feign), service discovery (Eureka), API Gateway
- [ ] Stock reservation and consistency between services
- [ ] Resilience4j (retry, circuit breaker)
- [ ] Security
- [ ] Docker and Docker Compose

## Author

Abhijit Soudi, Technology Analyst with experience in IBM Sterling OMS and Java, building skills in Spring Boot and microservices.
