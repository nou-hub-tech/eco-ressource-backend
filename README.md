# Eco-Ressource Backend

Spring Boot 3.2 / Java 17 / MySQL backend for the Eco-Ressource B2B
marketplace. JWT auth, role-based access, JPA, Swagger.

## Quick start

```bash
# 1. MySQL must be running locally — the JDBC URL auto-creates the DB:
#    jdbc:mysql://localhost:3306/eco_ressource_db?createDatabaseIfNotExist=true
#    Default user: root, no password (override in application.properties)

# 2. Build + run
mvn clean spring-boot:run

# Or run in two steps:
mvn clean package -DskipTests
java -jar target/backend-1.0.0-SNAPSHOT.jar
```

The API listens on `:8080`. Swagger UI: <http://localhost:8080/swagger-ui.html>.

## Demo accounts (auto-seeded on first boot)

| Email                  | Password  | Role          |
|------------------------|-----------|---------------|
| admin@marketplace.com  | admin123  | ADMIN         |
| slim@entreprise.tn     | demo123   | ENTERPRISE    |

## Modules

The backend is organised by feature:

- **Auth** — `/api/auth/**` — register / login / JWT issuance
- **Users / Enterprises / Transporters** — `/api/users/**`, `/api/enterprises/**`, `/api/transporters/**`
- **Listings & Marketplace** — `/api/listings/**`, `/api/products/**`, `/api/stock-items/**`, `/api/stock-movements/**`
- **Resource sharing** — `/api/resource-listings/**`, `/api/exchange-requests/**`
- **Group purchases** — `/api/groups/**`
- **Comments / Favorites** — `/api/comments/**`, `/api/favorites/**`
- **Logistics** — `/api/deliveries/**`, `/api/transport-offers/**`
- **Wallet** — `/api/wallet-transactions/**`
- **Eco circular module** — `/api/reservations/**`, `/api/eco-orders/**`, `/api/reservation-slots/**` ← see `BACKEND_ECO_MODULE.md`
- **Solidarity / Platform events** — `/api/solidarity-associations/**`, `/api/platform-events/**`
- **Admin dashboard** — `/api/admin/**`

## Eco circular module

The three entities required by the frontend's `pages/moduleReservation/`:

- **Reservation** (extended) — machine-time booking with eco metrics
- **EcoOrder** — procurement order with status workflow + eco-grade
- **ReservationSlot** — published machine availability with solar tagging
  and drag-&-drop booking support

Full API reference: [`BACKEND_ECO_MODULE.md`](./BACKEND_ECO_MODULE.md).

## Schema

`spring.jpa.hibernate.ddl-auto=update` — Hibernate evolves the schema on
each boot. For a clean install, drop the database; the JDBC URL has
`createDatabaseIfNotExist=true` so it'll come back fresh, and
`DatabaseSeed` + `EcoModuleSeed` will repopulate demo data.
