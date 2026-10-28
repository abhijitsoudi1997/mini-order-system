# mini-order-system

A small order management system built with **Spring Boot microservices**, inspired by my experience with IBM Sterling Order Management (OMS). The goal is to rebuild the core order flow (products, stock, orders) as independent services that talk to each other.

> **Status: work in progress.** The Inventory Service is running with an in-memory product API. The remaining services and features are on the roadmap below.

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
| inventory-service | 8081 | In progress (product CRUD, profiles, Actuator, logging) |
| order-service | 8082 | Planned |
| api-gateway | 8080 | Planned |
| Eureka server (service discovery) | 8761 | Planned |

Each service will own its database. Services will call each other over REST and be started together with Docker Compose.

## Tech stack

- Java 17+
- Spring Boot 4.x (Spring Web, Actuator)
- Maven (Maven Wrapper included)
- Planned: Spring Data JPA, MySQL, Spring Cloud (Eureka, Gateway, Resilience4j), Docker, JUnit 5 and Mockito

## Inventory Service

Manages the product catalogue. The data is held in memory for now, so it resets whenever the app restarts. A database is planned.

### API

| Method | URL | Description | Success status |
|---|---|---|---|
| GET | `/products` | List all products | 200 |
| GET | `/products?category=Electronics` | List products in a category | 200 |
| GET | `/products/{id}` | Get one product | 200 (404 if not found) |
| POST | `/products` | Create a product | 201 with a `Location` header |
| PUT | `/products/{id}` | Replace a product | 200 (404 if not found) |
| DELETE | `/products/{id}` | Delete a product | 204 (404 if not found) |
| GET | `/hello` | Returns the configured welcome message | 200 |
| GET | `/actuator/health` | Health check | 200 |

### Product JSON

```json
{
  "id": 1,
  "sku": "SKU-1001",
  "name": "Laptop",
  "category": "Electronics",
  "price": 55000.0,
  "quantity": 10
}
```

When creating a product, leave out `id`. The service assigns it.

### Example requests

```
# list products
curl http://localhost:8081/products

# create a product
curl -X POST http://localhost:8081/products \
  -H "Content-Type: application/json" \
  -d '{"sku":"SKU-1006","name":"Pen","category":"Stationery","price":20,"quantity":500}'

# delete a product
curl -X DELETE http://localhost:8081/products/6
```

## Run locally

Requirements: Java 17 or newer. Maven is not needed because the project includes the Maven Wrapper.

```
git clone https://github.com/abhijitsoudi1997/mini-order-system.git
cd mini-order-system/inventory-service
./mvnw spring-boot:run
```

The service starts on `http://localhost:8081`. You can also run `LearnSpringBootApplication` from Eclipse or IntelliJ.

### Profiles

Configuration is split by environment:

- `application.properties`: shared settings
- `application-dev.properties`: development settings (more logging, health details shown)
- `application-prod.properties`: production-style settings

Select one with `spring.profiles.active` in `application.properties`, or override it at run time:

```
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

## Roadmap

- [x] Inventory Service: product CRUD (in memory)
- [x] Profiles, Actuator and logging
- [ ] Spring Data JPA with H2, then MySQL
- [ ] Validation, global exception handling and Swagger/OpenAPI
- [ ] Unit and integration tests
- [ ] Order Service with orders and order lines
- [ ] Service-to-service calls (Feign), service discovery (Eureka), API Gateway
- [ ] Resilience4j (retry, circuit breaker)
- [ ] Security
- [ ] Docker and Docker Compose

## Author

Abhijit Soudi, Technology Analyst with experience in IBM Sterling OMS and Java, building skills in Spring Boot and microservices.
