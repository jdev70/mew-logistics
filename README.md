# Maritime Electromagnetic Warfare (MEW) Logistics & Readiness Dashboard

## Executive Summary
This project is a decoupled, enterprise-ready logistics and readiness dashboard designed to track Maritime Electromagnetic Warfare (MEW) assets (e.g., `AN/SLQ-32(V)6 SEWIP`) across the naval fleet. Built on a **Java EE 8 (`javax.*`) / Spring MVC / Hibernate JPA** backend and an **Angular 17 / RxJS / PrimeNG** frontend, the system aligns with **Department of Defense Architecture Framework (DoDAF)** viewpoints to support fleet readiness and depot sustainment operations.

---

## Development Handoff

### Current project position

The project is in the **demo-hardening and governed-data phase**. The core
joint-service backend and Angular dashboard are functional, with deterministic
fictional data for local demonstrations. The Java package root is now
`mil.dod.mew`; the former Navy-specific package root has been retired.

Implemented foundations include:

- JWT-backed RBAC/ABAC, resource-scoped catalog, asset, inventory, and
  work-order access controls.
- Deterministic synthetic personnel, installations, platforms, loadouts,
  inventory, maintenance, audit, and readiness data.
- Governed catalog provenance: versioned source manifest, source
  reconciliation/upsert workflow, `CatalogSource`, and per-target
  `CatalogAttribution` records.
- Structured maintenance lifecycle: technician submission, QA approval or
  rework, return-to-service, closure, and immutable lifecycle events.
- Lifecycle and supply analytics, including QA turnaround/aging, rework rate,
  requisition demand, fill rate, priority-one demand, and estimated lead time.
- Clickable supply analytics drill-downs grouped by installation.

The local/demo data remains fictional. Public catalog-source metadata is kept
separate from synthetic readiness, personnel, inventory, and maintenance data.

### Current operator notes

- Start the local backend with:

  ```powershell
  cd backend-java
  mvn spring-boot:run "-Dspring-boot.run.profiles=local"
  ```

- Start the frontend with `cd frontend` followed by `npm start`.
- When starting against an existing local H2 database, restart the backend
  after lifecycle-seed changes so new demonstration events are available.
- Use the raw API, Swagger UI, or H2 console for underlying data inspection;
  see the [raw-data guidance](#raw-data-inspection) below.

### Upcoming implementation components

The next work should follow [the future implementation roadmap](docs/FUTURE_IMPLEMENTATION_ROADMAP.md), with these priorities:

1. Complete lifecycle/logistics reconciliation tests so analytics totals are
   continuously checked against work orders, requisitions, inventory
   transactions, and readiness snapshots.
2. Expand supply drill-downs from installation summaries to component, fleet,
   and platform-level trends and detail tables.
3. Enrich synthetic personnel with qualifications, training currency,
   certifications, and authorized-absence readiness inputs.
4. Deliberately expand reviewed public catalog coverage through the manifest
   import workflow; do not add unattributed or operationally sensitive data.
5. Improve responsive UI ergonomics further, especially drill-down views on
   laptop-sized displays.

### Raw-data inspection

For local development, inspect JSON through `/api/v1/` endpoints or Swagger at
`http://localhost:8080/swagger-ui.html`. When the `local` profile enables H2,
use `http://localhost:8080/h2-console` and the active profile's JDBC settings.
Useful tables include `mew_work_order`, `mew_work_order_lifecycle_event`,
`mew_requisition`, `mew_inventory_balance`, `mew_personnel`, and
`mew_catalog_attribution`.

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

### Local Oracle XE (optional)

Create the local `MEW_APP` account once. The command prompts locally for the Oracle administrator password and the new application password; neither is stored in the command, repository, or chat:

```powershell
& 'C:\app\dataa\product\21c\dbhomeXE\bin\sqlplus.exe' 'sys@localhost:1521/XEPDB1 as sysdba' '@..\scripts\oracle\create-mew-app.sql'
```

After that succeeds, set the `MEW_APP` password only in your current PowerShell session and start with the Oracle profile:

```powershell
$env:MEW_ORACLE_LOCAL_PASSWORD = 'your-local-MEW_APP-password'
cd backend-java
mvn spring-boot:run "-Dspring-boot.run.profiles=oracle-local"
```

Alternatively, from the repository root, use the provided launcher. It prompts for the password without echoing it and removes the environment variable when the application stops:

```powershell
.\scripts\oracle\run-oracle-local.ps1
```

To run the repeatable Oracle schema/API smoke test, leave the password in the current PowerShell session and run:

```powershell
cd backend-java
mvn -Poracle-local-it verify
```

This test validates the existing Oracle schema (`ddl-auto: validate`) and performs read-only API checks. It does not recreate or drop Oracle tables.

Or, from the repository root, use the password-prompting launcher:

```powershell
.\scripts\oracle\test-oracle-local.ps1
```

The password is deliberately not stored in source control. The `oracle-local` profile uses `localhost:1521/XEPDB1`, applies development schema updates, and disables the H2 console.

The `local` profile is development-only and is the only profile that enables H2, automatic schema updates, its web console, and the configured demo identity. The base profile has no development datastore configuration.

For production, start with `--spring.profiles.active=prod`, set `MEW_JWT_ISSUER_URI` to the trusted identity-provider issuer, and optionally set `MEW_DATASOURCE_JNDI` (default: `jdbc/MewLogisticsDS`). The production profile disables H2 and uses `ddl-auto: validate`; apply [the authorization schema migration](docs/AUTHORIZATION_SCHEMA_MIGRATION.md) and provision each JWT `sub` as a unique `identity_subject` before deployment.

### Local Keycloak JWT testing (optional)

Start the free, local-only Keycloak development container. It listens only on `http://localhost:8180` and does not use Oracle Cloud or a paid identity provider:

```powershell
.\scripts\keycloak\start-keycloak-local.ps1
```

Open `http://localhost:8180`, sign in to the Keycloak administration console as `local-admin` using the password entered by the launcher, then create a user in the `mew-local` realm. After creating a password for that user, map the user's Keycloak `sub` to `mew_personnel.identity_subject` in the local Oracle database. Start the backend with both profiles:

For a local user named `mew-psm`, this helper enables the user, removes Keycloak required actions, and sets a non-temporary password:

```powershell
.\scripts\keycloak\provision-keycloak-local-user.ps1
```

Then follow [the local Keycloak-to-Oracle mapping guide](docs/LOCAL_KEYCLOAK_ORACLE_MAPPING.md) to map the printed JWT subject to the seeded Enterprise PSM record (`MEW-PER-0000`).

Then start the backend with both profiles:

```powershell
.\scripts\keycloak\run-oracle-keycloak-local.ps1
```

With the backend running, verify a real bearer-token request without printing the token:

```powershell
.\scripts\keycloak\test-keycloak-api.ps1
```

Verify that the current Keycloak user subject still matches the Oracle authorization assignment
without changing data:

```powershell
.\scripts\keycloak\verify-keycloak-oracle-mapping.ps1
```

Local Keycloak state is stored in Docker's `keycloak-data` named volume. Do not delete that volume
unless intentionally resetting local identity data; a newly created user can have a different subject
and must be mapped to Oracle again. See [the mapping guide](docs/LOCAL_KEYCLOAK_ORACLE_MAPPING.md).
If your running container predates this volume, its first recreation performs a one-time local user
reset; provision, map, and verify the user once more before using the persistent environment.

`start-dev` is strictly for local development; it is not a production Keycloak configuration.

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

# Start with local Keycloak browser sign-in and automatic API bearer tokens
# (requires the local Keycloak container and oracle-local,keycloak-local backend profiles)
npm run start:keycloak

# Production build check
npm run build
```

`npm run start:keycloak` uses Keycloak's authorization-code flow with PKCE. It redirects to the
local `mew-local` realm, keeps issued tokens in memory, refreshes them before API calls, and adds
the bearer token only to calls to `http://localhost:8080/api/v1`. Use the **Sign out** control in
the dashboard policy panel to end the local Keycloak session. The normal `npm start` command keeps
the existing non-Keycloak development configuration.

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
