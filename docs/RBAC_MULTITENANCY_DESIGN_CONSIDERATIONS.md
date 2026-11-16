# RBAC or Multi-Tenancy Design Considerations
1. Multi-Tenancy Boundary Definitions

In a defense logistics context, "tenants" rarely map to simple commercial companies. Instead, multi-tenancy should be structured around Command Echelons, Program Offices, and Coalition Boundaries.
• Hierarchical Tenant Scoping (Tree-Based Tenancy): Military logistics operates in a chain of command (e.g., Fleet Command → Strike Group → Hull / Squadron). Your database schema should support hierarchical tenancy so a Type Commander (TYCOM) or Fleet Logistician can view aggregated readiness across all subordinate units, while a shipboard Supply Officer is strictly isolated to their specific hull (hull_id).
• Program & Vendor Isolation: MEW systems involve sensitive proprietary hardware and classified performance metrics. OEM contractors, field service engineers (FSEs), and Warfare Center engineers (e.g., NSWC Crane or Dahlgren) should reside in segregated tenants or strict cross-tenant views. They should only see Line Replaceable Units (LRUs), failure rates, and telemetry tied to their specific program of record without exposing broader fleet force-disposition.
• Classification & Coalition Segregation: If the application handles Unclassified (FOUO/CUI) logistics alongside higher classification telemetry or allied Foreign Military Sales (FMS) partners, logical row-level separation is insufficient. Design for database-per-tenant or instance-per-enclave isolation at security boundaries, using unidirectional data diodes or strict API gateways to sync non-sensitive supply data upward or downward.

2. Hybrid Access Control (RBAC + ABAC)

Pure RBAC quickly leads to "role explosion" in military applications because permissions depend heavily on where a user is stationed, what billet they hold, and what operational phase a unit is in. Pair RBAC (functional duties) with Attribute-Based Access Control (ABAC) (contextual constraints).

Core Functional Roles (RBAC)

• Shipboard / Squadron Technician (e.g., CTT, ET, AT): Can view local system diagnostics, log maintenance actions, and initiate part requisitions for assigned systems. Cannot approve high-value transfers or view fleet-wide readiness.
• Unit Supply / Logistics Officer (SUPPO): Can approve requisitions, manage local onboard spares (COSAL/AVCALS), and track inbound shipments for their specific hull or squadron.
• Shore-Based Depot / Inventory Manager: Can view stock levels across regional warehouses, process NSN (National Stock Number) fulfillment, and manage repair turn-ins.
• Warfare Center / Systems Engineer: Read-only access to cross-fleet Mean Time Between Failures (MTBF), failure modes, and SWaP-C telemetry for reliability engineering, stripped of sensitive operational deployment coordinates.
• Fleet Readiness Analyst / Command Staff: Read-only, aggregated dashboards showing operational availability (\(A_{o}\)), casualty reports (CASREPs), and readiness heatmaps across a designated Area of Responsibility (AOR).

Contextual Attributes (ABAC)

• Deployment & Operational Status: If a hull enters a sensitive operational phase or EMCON (Emission Control) posture, real-time geospatial telemetry and exact port-of-call logistics data should automatically mask from lower-clearance shore users.
• Billet & UIC (Unit Identification Code) Assignment: Access should dynamically bind to a user's active UIC. When personnel transfer installations or rotate off a ship, revoking or updating their UIC attribute immediately shifts their data scope without manual role reconfiguration.
• CAGE Code / Contract Scope: External contractors must have an attribute matching their authorized CAGE codes so they can only query parts and maintenance logs for hardware they legally support.

3. Data-Level Security & Aggregation Risks

In MEW logistics, individual pieces of unclassified data can become classified when aggregated (the Mosaic Effect).
• Geospatial & Telemetry Masking: Real-time telemetry paired with exact coordinates reveals fleet movement and electronic order of battle (EOB). Implement dynamic field-level redaction so depot workers see where to ship a part (e.g., a Fleet Mail Center or homeport hub) without seeing the live GPS coordinates of the deployed vessel.
• CASREP & Readiness Aggregation: A single broken RF amplifier on one destroyer is routine maintenance. A dashboard showing that 60% of destroyers in a specific fleet have degraded EW capability is a major operational vulnerability. Enforce threshold-based access rules and strict auditing on bulk exports or cross-tenant analytical queries.
• Row-Level Security (RLS): Enforce tenant and UIC filtering directly at the database layer (e.g., PostgreSQL RLS policies) rather than relying solely on application-layer middleware. This guarantees that background analytics jobs, reporting engines, or API endpoints cannot accidentally leak cross-hull data.

4. Architectural Implementation Summary


Design Layer	Recommended Pattern	MEW Logistics Application
Tenant Model	Hierarchical Tree + Siloed Enclaves	Parent-child mapping for Fleet → Hull, with hard isolation for external OEMs and classification tiers.
Authorization	RBAC + ABAC Policy Engine (e.g., OPA)	Roles define actions (approve requisition); attributes define scope (UIC, CAGE_code, Clearance, EMCON_state).
Data Isolation	Database Row-Level Security (RLS)	Every LRU, requisition, and telemetry record carries tenant_id and uic tags enforced on every query.
Privacy / OPSEC	Dynamic Field-Level Masking	Hides live vessel lat/long and mission-specific threat library versions from supply-chain and depot roles.
Auditability	Immutable Event Logging	Tracks every cross-unit query, CASREP view, and readiness export for OPSEC and compliance reviews.
