# E-commerce Distributed Platform

A microservices-based e-commerce backend built with Spring Boot, demonstrating distributed systems patterns: service-to-service communication, event-driven messaging, caching, observability, and performance testing under load.

Built as a portfolio project to practice and demonstrate backend engineering beyond a single-service CRUD app.

## Architecture

```
                    ┌─────────────┐
                    │ API Gateway │  :8000
                    └──────┬──────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
        ▼                  ▼                  ▼
 ┌─────────────┐   ┌─────────────┐   ┌──────────────┐
 │Users Service│   │ Products    │   │ Orders       │
 │   :8080     │   │ Service     │   │ Service      │
 │             │   │   :8081     │   │   :8082      │
 └──────┬──────┘   └──────┬──────┘   └──────┬───────┘
        │                 │                  │
        ▼                 ▼                  ▼
   ┌────────┐        ┌────────┐         ┌────────┐
   │users-db│        │products│         │orders-db│
   │Postgres│        │  -db   │         │Postgres │
   └────────┘        │Postgres│         └────────┘
                      └───┬────┘              │
                          ▼                   ▼
                      ┌───────┐          ┌─────────┐
                      │ Redis │          │  Kafka  │
                      │(cache)│          │         │
                      └───────┘          └────┬────┘
                                               │
                                               ▼
                                    ┌──────────────────┐
                                    │Notifications      │
                                    │Service :8083       │
                                    └────────────────────┘
```

Each service owns its own PostgreSQL database (database-per-service pattern). `orders-service` calls `users-service` and `products-service` synchronously via REST to validate a request, then publishes an `OrderCreated` event to Kafka. `notifications-service` consumes that event asynchronously — it has no direct dependency on `orders-service`.

## Tech stack

- **Java 21** / **Spring Boot 3** (Spring Web, Spring Data JPA, Spring Validation, Spring Boot Actuator)
- **Spring Cloud Gateway** — single entry point, routes `/api/users/**`, `/api/products/**`, `/api/orders/**`
- **PostgreSQL** — one instance per service
- **Redis** — read-through cache on the product catalog (`@Cacheable`)
- **Apache Kafka** (KRaft mode) — asynchronous order events; [Kafka UI](https://github.com/provectus/kafka-ui) included for inspecting topics
- **Docker Compose** — full local environment, one command to run everything
- **Prometheus + Grafana** — metrics collection and dashboards (via Micrometer / Actuator)
- **k6** — load and stress testing, with root-cause analysis of a real bottleneck found under load (see [RESULTS.md](./RESULTS.md))

## Running it

Requires Docker Desktop.

```bash
git clone https://github.com/AnnaMartina31/ecommerce-platform.git
cd ecommerce-platform
docker compose up -d
```

This starts all services, their databases, Redis, Kafka, Prometheus, and Grafana.

| Service | URL |
|---|---|
| API Gateway | http://localhost:8000 |
| Kafka UI | http://localhost:8090 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 (login: `admin` / `admin`) |

Example request through the gateway:
```bash
curl http://localhost:8000/api/products
```

## Performance testing

The [`k6-tests/`](./k6-tests) folder contains smoke, load, and stress test scripts for both the read path (`GET /api/products`, Redis-cached) and the write path (`POST /api/orders`, which touches two services, Postgres, and Kafka).

Stress testing surfaced a real bottleneck: the default HikariCP connection pool (10 connections) saturated under concurrent order creation, causing request timeouts. The pool size was increased and the fix verified with a clean before/after benchmark comparison.

Full methodology, numbers, and root-cause analysis: **[RESULTS.md](./RESULTS.md)**

## What's not (yet) included

- Kubernetes deployment (the platform currently runs via Docker Compose only)
- Automated test suite (unit/integration tests exist per-service but aren't wired into CI)
- Distributed tracing (Zipkin/Jaeger)
- Authentication/authorization on the gateway

## Project structure

```
ecommerce-platform/
├── api-gateway/
├── users-service/
├── products-service/
├── orders-service/
├── notifications-service/
├── k6-tests/              # k6 load/stress test scripts
├── docker-compose.yml
├── prometheus.yml
├── RESULTS.md             # performance testing report
└── README.md
```
