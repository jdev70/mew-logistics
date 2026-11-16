# Maritime Electromagnetic Warfare (MEW) Logistics & Readiness Dashboard

## Executive Summary
This project is a decoupled, enterprise-ready logistics and readiness dashboard designed to track Maritime Electromagnetic Warfare (MEW) assets (e.g., `AN/SLQ-32(V)6 SEWIP`) across the naval fleet. Built on a **Java EE 8 (`javax.*`) / Spring MVC / Hibernate JPA** backend and an **Angular 17 / RxJS / PrimeNG** frontend, the system aligns with **Department of Defense Architecture Framework (DoDAF)** viewpoints to support fleet readiness and depot sustainment operations.

---

## DoDAF Architectural Alignment

* **CV-1 (Vision):** Provide logistics officers, depot managers, and product support managers with real-time visibility into MEW system deployment, maintenance, and operational availability (`Ao`).
* **OV-1 (High-Level Operational Concept):** Track MEW hardware lifecycles from depot-level sustainment (NSWC Crane) to active fleet deployment, surfacing degraded systems and supply bottlenecks before they impact mission capability.
* **DIV-2 (Logical Data Model):** Relational schema implemented via **Hibernate / JPA (`javax.persistence.*`)** modeling `Installation`, `MilitaryUnit`, `NavalHull`, `Personnel`, `AssetModel`, `MaterialCatalog`, `Asset`, `HullSystemAssignment`, `WorkOrder`, `WorkOrderPart`, `Requisition`, `InventoryBalance`, `InventoryTransaction`, `UnitReadinessSnapshot`, and `SecurityAuditLog`.
* **SvcV-1 (Services Context Description):** Decoupled RESTful API layer implemented with pure **Spring MVC `@RestController`s** under `/api/v1/`, enforcing persona-scoped **RBAC/ABAC** policies and `SNAKE_CASE` JSON contracts.
* **SV-4 (Systems Functionality Description):** Reactive **Angular 17** client using Signals, `HttpClient`, **RxJS**, **PrimeNG** (`p-table`), and **Tailwind CSS** to deliver real-time filtering, analytics, and persona-gated CRUD workflows.

---

## Technology Stack

* **Backend (SvcV / DIV):** Java 17 (Java EE 8 `javax.*` target), Spring Boot 2.7.18, Spring MVC REST, Spring Data JPA / Hibernate 5.6, Maven (`WAR` packaging for WebLogic / JBoss or standalone execution)
* **Database (DIV):** H2 (`MODE=Oracle`, file-backed local persistence at `./data/mewdb`), JNDI profile (`jdbc/MewLogisticsDS`) for Java EE application servers
* **Testing:** JUnit 5 (Jupiter), Mockito, Spring `MockMvc` (`@WebMvcTest`)
* **Frontend (SV):** Angular 17.3, TypeScript, RxJS 7.8, PrimeNG 17, Tailwind CSS

---

## System Architecture & Core Patterns

* **Authenticated RBAC / ABAC ([MewAuthorizationService.java](backend-java/src/main/java/mil/navy/nswc/mew/security/MewAuthorizationService.java)):** Production API requests require a validated bearer JWT. Its verified subject resolves to a persisted personnel authorization assignment (branch, command tier, clearance, installation/CAGE scope, and permissions); asset and work-order access is also checked at the resource scope. Legacy `X-MEW-Persona-ID` values are ignored.
* **Immutable Inventory Ledger ([InventoryService.java](backend-java/src/main/java/mil/navy/nswc/mew/service/InventoryService.java)):** Uses `PESSIMISTIC_WRITE` locking on `InventoryBalance` and appends immutable `InventoryTransaction` records (`@PreUpdate` blocks mutations).
* **MILSTRIP Short-Ship Backorder Splitting ([RequisitionService.java](backend-java/src/main/java/mil/navy/nswc/mew/service/RequisitionService.java)):** Receiving a partial shipment automatically splits the remaining quantity into a `-BO` backorder requisition (`BB` status) and credits warehouse stock (`D7` status).
* **Automated Readiness Snapshots ([ReadinessSchedulerService.java](backend-java/src/main/java/mil/navy/nswc/mew/service/ReadinessSchedulerService.java)):** `@Scheduled` cron job computing daily `UnitReadinessSnapshot` records.
* **Local Dev Seeder ([DevDataInitializer.java](backend-java/src/main/java/mil/navy/nswc/mew/config/DevDataInitializer.java)):** Seeds sample installations, naval hulls, MEW assets, LRU inventory balances, work orders, and requisitions on first startup when using the `default` profile.

---

## Frequently Used Commands

### Backend (`backend-java/`)

> If `mvn` is not in your current shell `PATH`, invoke it via `"$env:USERPROFILE\tools\maven\apache-maven-3.9.9\bin\mvn.cmd"`.

```powershell
cd backend-java

# Run JUnit 5 + Mockito unit & MockMvc tests
mvn test

# Run tests offline (after initial dependency download)
mvn -o test

# Package deployable WAR (target/mew-logistics-api.war)
mvn package

# Run locally on http://localhost:8080 (H2 file-backed DB + DevDataInitializer + demo identity)
mvn spring-boot:run "-Dspring-boot.run.profiles=local"

# Run packaged WAR directly
java -jar target\mew-logistics-api.war --spring.profiles.active=local
```

The `local` profile is development-only and is the only profile that enables H2, automatic schema updates, its web console, and the configured demo identity. The base profile has no development datastore configuration.

For production, start with `--spring.profiles.active=prod`, set `MEW_JWT_ISSUER_URI` to the trusted identity-provider issuer, and optionally set `MEW_DATASOURCE_JNDI` (default: `jdbc/MewLogisticsDS`). The production profile disables H2 and uses `ddl-auto: validate`; apply [the authorization schema migration](docs/AUTHORIZATION_SCHEMA_MIGRATION.md) and provision each JWT `sub` as a unique `identity_subject` before deployment.

### H2 Database Console (Local Dev)
While the backend is running, open **`http://localhost:8080/h2-console`**:
* **JDBC URL:** `jdbc:h2:file:./data/mewdb;MODE=Oracle;AUTO_SERVER=TRUE`
* **User Name:** `sa`
* **Password:** *(leave blank)*

### Frontend (`frontend/`)

```powershell
cd frontend

# Clean install exact locked Angular 17 dependencies
npm ci

# Start dev server on http://localhost:4200
npm start

# Production build check
npm run build
```

---

## REST API Routing (`SvcV-1` Services Context)

Base URL: `http://localhost:8080/api/v1`

* **Dashboard & Analytics:**
  * `GET /dashboard/units/` (`?asset_class=...`) & `GET /dashboard/units/export_csv/`
  * `GET /analytics/overview/`
* **MEW Assets (Full CRUD):**
  * `GET /assets/`, `GET /assets/{id}/`, `POST /assets/`, `PUT /assets/{id}/`, `DELETE /assets/{id}/`
* **Work Orders (Full CRUD + Lifecycle):**
  * `GET /work-orders/`, `GET /work-orders/{id}/`, `POST /work-orders/`, `PUT /work-orders/{id}/`, `DELETE /work-orders/{id}/`, `POST /work-orders/{id}/close/`
* **Requisitions & Receiving (Full CRUD + MILSTRIP Receipt):**
  * `GET /requisitions/`, `GET /requisitions/{id}/`, `POST /requisitions/`, `PUT /requisitions/{id}/`, `DELETE /requisitions/{id}/`
  * `POST /requisitions/receive-shipment/`
* **Inventory & Audit Trails:**
  * `GET /inventory-balances/` & `GET /inventory-transactions/`
  * `GET /security-audit-logs/` & `GET /rbac/personas/`
* **Catalogs (Full CRUD):**
  * `/installations/`, `/personnel/`, `/hulls/`, `/components/` (plus `GET /loadouts/`)
