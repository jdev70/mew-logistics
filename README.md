# Maritime Electromagnetic Warfare (MEW) Logistics & Readiness Dashboard

## Executive Summary
This project is a prototype logistics and readiness dashboard designed to track Maritime Electromagnetic Warfare (MEW) assets (e.g., AN/SLQ-32(V)6 SEWIP) across the naval fleet. Built using a decoupled Angular and Django REST Framework (DRF) architecture, this application demonstrates how modern, rapid software development can be aligned with strict Department of Defense Architecture Framework (DoDAF) interoperability requirements to support operations at NSWC Crane.

---

## DoDAF Architectural Alignment

To ensure data interoperability and strict traceability to mission requirements, this application is modeled against the following DoDAF viewpoints:

### Strategic & Operational 
*   **CV-1 (Vision):** Provide logistics officers and product support managers with real-time visibility into MEW system deployment, maintenance, and operational statuses to ensure continuous fleet readiness.
*   **OV-1 (High-Level Operational Concept):** Track the lifecycle of MEW hardware from depot-level maintenance (NSWC Crane) to active fleet deployment, enabling proactive identification of degraded systems before they impact mission capabilities.

### Technical & Systems
*   **DIV-2 (Logical Data Model):** A relational data architecture (implemented via PostgreSQL and Django Models) tracking MEW Assets, Naval Platforms (Vessels), and Operational Statuses. The schema is designed for eventual mapping to the DoDAF Meta Model (DM2).
*   **SvcV-1 (Services Context Description):** A decoupled RESTful API layer (implemented via Django DRF). By separating the data layer from the user interface, the API can act as a microservice, allowing other DoD systems to securely consume MEW readiness data.
*   **SV-4 (Systems Functionality Description):** A reactive client interface (implemented via Angular 17). It consumes the SvcV-1 API to render a dynamic, sortable PrimeNG data grid (`p-table`), allowing end-users to filter fleet readiness metrics in real-time.

---

## Agile Execution Strategy

Development was executed using iterative Agile methodologies to ensure continuous delivery of value without waiting for monolithic architecture approvals.

*   **Epic 1: Data Interoperability & API Services (Sprint 1)**
    *   Designed the DIV-2 PostgreSQL database schema.
    *   Built the Django backend and DRF serializers.
    *   Seeded the database with synthetic MEW asset and deployment data.
    *   Exposed the SvcV-1 REST endpoints (`/api/mew-assets/`).
*   **Epic 2: Systems Functionality & UI Integration (Sprint 2)**
    *   Scaffolded the Angular 17+ workspace using Signals for state management.
    *   Integrated PrimeNG component libraries and Tailwind CSS.
    *   Implemented `HttpClient` services to consume the DRF API.
    *   Delivered the SV-4 interactive data table for logistics filtering.

---

## Technology Stack

*   **Frontend (SV):** Angular 17 (Signals), PrimeNG (`p-table`), Tailwind CSS, RxJS.
*   **Backend (SvcV/DIV):** Python 3.11+, Django 5.1, Django REST Framework (DRF).
*   **Database (DIV):** PostgreSQL / SQLite (Local Dev).

---

## Local Development Setup

### Backend (Django & SvcV-1 API)
```powershell
python -m venv .venv
.\.venv\Scripts\activate
pip install -r backend\requirements.txt
python backend\manage.py migrate

# Seed the full MEW dataset (Installations, Personnel, Naval Hulls/Systems, Components & Analytics)
python backend\manage.py seed_mew_data

# Or run individual MEW synthetic dataset generators (also exports CSV & JSON):
python backend\manage.py generate_mew_installations
python backend\manage.py generate_mew_personnel --count 1000
python backend\manage.py generate_mew_loadouts
python backend\manage.py generate_mew_components

python backend\manage.py runserver
```

### Frontend (Angular 17+ SV-4 Tactical Dashboard)
```powershell
cd frontend
npm install
npm start
# Open http://localhost:4200
```

---

## Routing Structure (`SvcV-1` Services Context)
**Maximizes extensibility** by isolating the MEW logistics routes behind the `/api/v1/` path in `core/urls.py` and delegating specifics to `mew_logistics/urls.py`, ensuring that future modules (e.g., if NAVSEA requests functionality to track Sonar or Radar systems next) can be added cleanly without disrupting existing Maritime Electromagnetic Warfare services:

*   `GET /api/v1/dashboard/units/` — Unit readiness aggregation, 30-day readiness trends, and `MTBF`/`MTTR`/`Ao` metrics (`?asset_class=...`).
*   `GET /api/v1/dashboard/units/export_csv/` — Downloadable CSV report of fleet reliability metrics.
*   `GET /api/v1/assets/` — Deployed MEW system instances (`AN/SLQ-32(V)6/7`, `Nulka`, `ODIN`, `AN/BLQ-10`, `AN/ALQ-249`) with active work orders and pending requisitions.
*   `GET /api/v1/hulls/` & `GET /api/v1/loadouts/` — Naval Hulls (`DDG-51`, `CVN-78`, `SSN-774`, `VAQ-132`) and Hull-to-System loadout assignments.
*   `GET /api/v1/installations/` & `GET /api/v1/personnel/` — 17 MEW shore installations and EW personnel billets (`CTT`, `EWO`, `EDO`, `GS-0855`, `GS-0346`, `FSR`).
*   `GET /api/v1/components/` & `GET /api/v1/inventory-balances/` — MEW Line Replaceable Units (LRUs) catalog and warehouse stock levels.
*   `GET /api/v1/requisitions/` & `POST /api/v1/requisitions/receive-shipment/` — MILSTRIP supply requisitions and scanner-ready warehouse shipment receipt (with automatic short-ship `-BO` backorder splitting).
*   `GET /api/v1/work-orders/` & `GET /api/v1/inventory-transactions/` — Maintenance work orders and the immutable inventory audit ledger.

