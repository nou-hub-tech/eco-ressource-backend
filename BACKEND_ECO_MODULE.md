# Eco Module — Backend API

This document covers the three new entities added to the marketplace backend:
**Reservation** (extended), **EcoOrder** (new), **ReservationSlot** (new).

All endpoints require authentication. The `Authorization: Bearer <jwt>` header
is enforced by the existing `JwtAuthFilter`. Requests resolve the caller's
enterprise from the `User → Enterprise` link.

---

## File map

```
src/main/java/com/marketplace/backend/
├── entity/
│   ├── Reservation.java                  ← extended (eco fields + soft delete)
│   ├── EcoOrder.java                     ← NEW
│   ├── ReservationSlot.java              ← NEW
│   └── enums/
│       ├── ReservationStatus.java        ← +cancelled
│       ├── OrderStatus.java              ← NEW
│       ├── EcoGrade.java                 ← NEW
│       └── SlotStatus.java               ← NEW
├── dto/
│   ├── ReservationRequest.java           ← extended
│   ├── CancelReservationRequest.java     ← NEW
│   ├── EcoOrderRequest.java              ← NEW
│   ├── CancelOrderRequest.java           ← NEW
│   ├── ReservationSlotRequest.java       ← NEW
│   ├── CancelSlotRequest.java            ← NEW
│   └── SlotBookRequest.java              ← NEW
├── repository/
│   ├── ReservationRepository.java        ← +findActive*
│   ├── EcoOrderRepository.java           ← NEW
│   └── ReservationSlotRepository.java    ← NEW
├── service/
│   ├── ReservationService.java           ← +softDelete, +eco fields, +includeDeleted
│   ├── EcoOrderService.java              ← NEW
│   └── ReservationSlotService.java       ← NEW
├── controller/
│   ├── ReservationController.java        ← +/cancel, includeDeleted query
│   ├── EcoOrderController.java           ← NEW
│   └── ReservationSlotController.java    ← NEW
├── config/
│   ├── SecurityConfig.java               ← +/api/eco-orders, +/api/reservation-slots
│   └── EcoModuleSeed.java                ← NEW (demo data)
```

---

## REST endpoints

### Reservation — `/api/reservations`

| Method | Path                  | Body                       | Returns        | Notes |
|--------|-----------------------|----------------------------|----------------|-------|
| GET    | `/` (`?includeDeleted=`) | —                       | `Reservation[]` | Soft-deleted hidden by default |
| GET    | `/{id}`               | —                          | `Reservation`  | |
| POST   | `/`                   | `ReservationRequest`       | `Reservation` (201) | Creates an active reservation |
| PUT    | `/{id}`               | `ReservationRequest`       | `Reservation`  | 400 if already cancelled |
| **POST** | **`/{id}/cancel`**  | `CancelReservationRequest` | `Reservation`  | **NEW** — soft delete with reason |
| DELETE | `/{id}`               | —                          | `204`          | Hard delete (admin/cleanup) |

**ReservationRequest** — existing fields plus optional eco extensions:
```json
{
  "typeLabel": "Machine slot",
  "item": "Injection Molder X2",
  "companyName": "EcoPlast",
  "fromDate": "2026-04-26",
  "toDate":   "2026-04-26",
  "price":    180.00,
  "status":   "confirmed",
  "enterpriseId": 1,

  "machine":   "Injection Molder X2",
  "hours":     6,
  "startHour": 3,
  "solar":     false,
  "co2Saved":  42.50
}
```

`status` allows: `confirmed | active | pending | completed | cancelled`.

---

### EcoOrder — `/api/eco-orders`

| Method | Path                | Body                | Returns      | Notes |
|--------|---------------------|---------------------|--------------|-------|
| GET    | `/` (`?includeDeleted=`) | —              | `EcoOrder[]` | |
| GET    | `/{id}`             | —                   | `EcoOrder`   | |
| POST   | `/`                 | `EcoOrderRequest`   | `EcoOrder` (201) | `ref` and `grade` are auto-generated if omitted |
| PUT    | `/{id}`             | `EcoOrderRequest`   | `EcoOrder`   | 400 if cancelled |
| POST   | `/{id}/advance`     | —                   | `EcoOrder`   | Workflow step: `draft → confirmed → shipped → delivered` |
| POST   | `/{id}/cancel`      | `CancelOrderRequest`| `EcoOrder`   | Soft delete with reason |
| DELETE | `/{id}`             | —                   | `204`        | Hard delete |

**EcoOrderRequest:**
```json
{
  "ref": "ORD-2045",          // optional — auto-generated
  "companyName": "EcoPlast",
  "material":    "Recycled HDPE",
  "qtyKg":       250,
  "supplier":    "PolyRecycle Tunis",
  "distanceKm":  72,
  "orderDate":   "2026-04-26",
  "status":      "confirmed",
  "grade":       "A",         // optional — inferred from material+distance
  "co2Saved":    450,
  "waterSaved":  1200,
  "wasteAvoided": 80,
  "enterpriseId": 1
}
```

`status` allows: `draft | confirmed | shipped | delivered | cancelled`.
`grade` allows: `A | B | C | D | E`.

The server-side grade inference uses simple heuristics (recycled / bio
materials and short transport distances → A; virgin materials and long
hauls → E). The frontend has its own richer grader; whichever value is
posted wins.

---

### ReservationSlot — `/api/reservation-slots`

| Method | Path                  | Body                       | Returns               | Notes |
|--------|-----------------------|----------------------------|-----------------------|-------|
| GET    | `/` (`?includeDeleted=`)| —                        | `ReservationSlot[]`  | |
| GET    | `/range?from=&to=`    | —                          | `ReservationSlot[]`  | All enterprises, in date range — heatmap query |
| GET    | `/{id}`               | —                          | `ReservationSlot`    | |
| POST   | `/`                   | `ReservationSlotRequest`   | `ReservationSlot` (201)| |
| PUT    | `/{id}`               | `ReservationSlotRequest`   | `ReservationSlot`    | 400 if deleted |
| POST   | `/{id}/book`          | `SlotBookRequest`          | `ReservationSlot`    | Drag-&-drop assignment from heatmap |
| POST   | `/{id}/toggle`        | —                          | `ReservationSlot`    | Toggle `open ↔ blocked` |
| POST   | `/{id}/cancel`        | `CancelSlotRequest`        | `ReservationSlot`    | Soft delete |
| DELETE | `/{id}`               | —                          | `204`                | Hard delete |

**ReservationSlotRequest:**
```json
{
  "machine":     "Injection Molder X2",
  "date":        "2026-04-26",
  "startHour":   11,                 // 0..23
  "endHour":     15,                 // 1..24, must be > startHour
  "status":      "open",             // optional, defaults to "open"
  "solar":       true,
  "discountPct": 25,                 // 0..100
  "owner":       "EcoPlast",
  "reservedBy":  null,
  "enterpriseId": 1
}
```

`status` allows: `open | booked | blocked`.

**SlotBookRequest** (drag-&-drop):
```json
{ "reservedBy": "GreenSteel" }
```

---

## Soft-delete semantics

All three entities have `deleted` (Boolean) + `cancelReason` (String) columns.

- **Read APIs** filter out soft-deleted rows by default. Pass
  `?includeDeleted=true` to see audit data.
- **Update APIs** refuse to mutate soft-deleted rows (HTTP 400).
- The **`/cancel`** endpoint is the canonical "delete" — the frontend's
  delete button calls this.
- The **`DELETE /{id}`** endpoint hard-deletes — useful for admin cleanup,
  not used by the frontend.

The repository queries are NULL-tolerant (`WHERE deleted IS NULL OR
deleted = false`), so legacy rows that predate this migration aren't
silently hidden.

---

## Authorization

`SecurityConfig` was updated with three new entries:

```java
.requestMatchers("/api/reservations/**").authenticated()
.requestMatchers("/api/eco-orders/**").authenticated()
.requestMatchers("/api/reservation-slots/**").authenticated()
```

Per-row access control happens in each service via `assertCanRead`:
- `ROLE_ADMIN` users see/edit everything.
- Enterprise users only see their own enterprise's rows.

---

## Demo data

`EcoModuleSeed` (a `@Configuration` with a `@Bean CommandLineRunner` ordered
after the existing `DatabaseSeed`) populates 11 slots, 4 orders, and 2
eco-flavoured reservations on a fresh DB. Re-running the app does not
duplicate — each block guards on `repo.count() == 0`.

---

## Schema migration

The project uses `spring.jpa.hibernate.ddl-auto=update`, so on first boot
Hibernate will:
- Create new tables `eco_orders` and `reservation_slots`.
- Add columns `machine`, `hours_count`, `start_hour`, `solar`, `co2_saved`,
  `deleted`, `cancel_reason` to the existing `reservations` table.

If you want a clean install, drop the database and let the seed re-create
it (`createDatabaseIfNotExist=true` is already in the JDBC URL).

---

## Local build (caveat)

The container I authored these files in does not have access to Maven
Central, so I could not run `mvn compile` to verify the classpath. The
code is written against the same dependency set the existing project
already uses (Spring Boot 3.2, Lombok, Spring Data JPA, Jakarta
Validation), and every project-internal import has been verified to
resolve to a real file. On your machine:

```
mvn clean compile      # quick check
mvn spring-boot:run    # boot the app on :8080
```

Swagger UI: <http://localhost:8080/swagger-ui.html> (the existing OpenAPI
config picks up the new endpoints automatically).

---

## Frontend wiring (TL;DR)

The Angular module under `pages/moduleReservation/` currently uses
in-memory mock data. To switch to live data, change the three services
(or stores) to call:

- `GET /api/reservations` — list
- `POST /api/reservations` — create
- `PUT /api/reservations/{id}` — edit
- `POST /api/reservations/{id}/cancel` — soft delete (with `{reason}` body)

…and the analogous endpoints for `/api/eco-orders` and
`/api/reservation-slots`. The DTO field names match the frontend's
TypeScript interfaces (`machine`, `hours`, `startHour`, `solar`,
`co2Saved`, `qtyKg`, `distanceKm`, `discountPct`, …) so most of the
mapping is a 1-to-1 swap.
