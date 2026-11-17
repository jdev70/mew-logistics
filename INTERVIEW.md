# Warrant Technologies Interview Brief

## Quick Navigation

- [Opening statement](#opening-statement)
- [Role alignment](#role-alignment)
- [Honest positioning](#honest-positioning)
- [Panel talking points](#panel-talking-points)
- [Six-minute demo](#six-minute-demo)
- [Pre-demo checklist](#pre-demo-checklist)
- [Recovery line](#recovery-line)

## Opening Statement

> I built this as a focused modernization case study after researching Warrant's work in maritime electromagnetic warfare product support and logistics. The role description stood out because it calls for Angular modernization, Java and Spring integration, JPA, Oracle, testing, and secure maintainable development. I used this project to show that I can work across those layers while being honest about the areas where I am still growing.

## Role Alignment

The position calls for Angular, RxJS, PrimeNG, JPA, HTML/CSS, multi-tier development, Java and Spring REST integration, testing, Maven, Oracle, Git, Agile collaboration, and legacy modernization.

| Job requirement | Evidence in the application | Suggested wording |
| --- | --- | --- |
| Angular, HTML, CSS, PrimeNG | Angular 17 dashboard, responsive views, PrimeNG tables, forms, filters, and role-aware UI | "I built the user experience in Angular with reusable state, forms, and PrimeNG tables." |
| RxJS | Angular API service uses HTTP and combined telemetry loading | "I used Angular's reactive patterns to load and coordinate dashboard data." |
| JPA and multi-tier design | Angular UI -> Spring Boot REST API -> services -> JPA repositories -> Oracle | "I intentionally separated presentation, application logic, security, and persistence." |
| Java and Spring REST integration | Spring controllers, services, DTOs, validation, and REST endpoints | "The Angular UI integrates with Java and Spring REST endpoints rather than relying on static data." |
| Legacy modernization | Reviewed legacy Django artifacts and moved useful business concepts into the Java and Angular architecture | "I treated the legacy code as a source of business rules and data concepts, not as something to blindly copy." |
| Testing and troubleshooting | Maven tests, JUnit/Mockito, integration tests, Oracle and Keycloak diagnostics | "I used tests and incremental diagnostics while working through real integration failures." |
| Oracle and SQL | Local Oracle XE, `MEW_APP` schema, SQL*Plus mapping, and JPA persistence | "I set up a local Oracle-backed environment to understand real persistence and identity-mapping behavior." |
| Security standards | Keycloak JWT authentication, Oracle authorization mapping, RBAC, scoped assets, and audit records | "I distinguished authentication from authorization and implemented role separation in the workflow." |
| Maintainability and documentation | Local runbooks, mapping guide, demo guide, and structured work-order DTOs | "I documented setup and operational decisions so another developer could reproduce the environment." |

## Honest Positioning

### Be direct about growth areas

> I am early in my hands-on Java, Oracle, and MEW domain experience. I would not present this as production-certified or claim subject-matter authority. What I can demonstrate is how I learn: I take a real operational workflow, research it, build a secure working vertical slice, test it, document what I learn, and improve it when integration reveals gaps.

### If asked about the three-year requirement

> I understand the role seeks an experienced developer. I would be candid about where my direct production experience is strongest and where I am still developing. This project is evidence of how quickly I can become productive in an unfamiliar stack while staying disciplined about testing, documentation, and asking the right questions.

> **Reminder:** Only state that you hold an active DoD Secret clearance if that is true.

## Panel Talking Points

- The role centers on legacy modernization. This project gave me a small-scale version of that problem: retain useful business concepts from legacy Django code while moving to an Angular, Java, JPA, and Oracle architecture.
- I wanted the application to demonstrate a complete workflow, not only a dashboard: technician work, submitted evidence, QA review, MAF closure, audit history, and changing metrics.
- The harder problem was not creating another CRUD screen; it was preserving separation of duties and traceability.
- Keycloak authenticates the user, while Oracle resolves that identity to a scoped personnel assignment. That distinction became real during implementation.
- The integration issues became learning opportunities: repository structure, Maven profiles, Oracle connections, JWT claims, Keycloak provisioning, and database mappings required methodical troubleshooting.
- In production, I would validate service-specific forms and maintenance codes with customer SMEs rather than claiming the prototype is an authoritative Navy record.

## Six-Minute Demo

### 1. Context - 0:30

> This is a MEW logistics and readiness prototype. I designed it around a mission-support workflow: a technician records maintenance, QA reviews it, the application preserves traceability, and readiness stakeholders can see the operational state.

### 2. Architecture - 0:45

> The presentation layer is Angular with PrimeNG components. It calls Java Spring Boot REST endpoints. The backend separates controller, service, and JPA repository responsibilities, and Oracle stores the operational and authorization data. Keycloak handles authentication.

### 3. Modernization angle - 0:30

> I also reviewed the legacy Django implementation. Rather than trying to run incompatible legacy loaders against Oracle, I moved the relevant domain concepts into the Java architecture and used native Java seed data. That is the same mindset I would bring to a modernization effort: preserve the intent, modernize the implementation.

### 4. Technician workflow - 1:30

**Do:** Sign in as `mew-tech` and open **Supply & Maintenance -> Work Orders -> `N21944-CE02-0002`**.

> I am now signed in as a scoped shipboard technician. This work order is assigned to the technician and is in work. The completion form guides the technician through diagnostics, safety and tag-out, isolation, fault identification, repair, restoration and testing, final equipment status, and labor.

**Do:** Complete the structured fields and select **Submit Completed Maintenance for QA**.

> Submitting it moves the MAF to QA pending and retains both the structured fields and a readable completion narrative.

### 5. Lifecycle analytics and auditability - 0:45

**Do:** Point to the work-order metrics and audit ledger.

> The work-order view distinguishes in-work actions, parts dependencies, QA backlog, and closed history. The audit records provide traceability across assignment, submission, QA, and closure.

### 6. QA closure - 1:00

**Do:** Sign out, sign in as `mew-psm`, and open the submitted record.

> Now I am acting as the fleet readiness reviewer. The technician can submit maintenance, but a reviewer performs QA acceptance and closure. That reflects separation of duties rather than allowing one person to self-approve a completed maintenance action.

**Do:** Select **Accept QA and Close MAF**.

### 7. Close - 0:20

> The point of the demo is not that this is a finished enterprise product. It is that I can understand a workflow, translate it into a maintainable multi-tier application, test it, and communicate the design decisions clearly.

## Pre-Demo Checklist

- [ ] Restart the backend and frontend once.
- [ ] Sign in as `mew-tech` and confirm `N21944-CE02-0002` is `IN_WORK`.
- [ ] Keep a short technician resolution narrative ready to paste into each form section.
- [ ] Confirm `mew-psm` can sign in for QA closure.
- [ ] Keep the demo under six minutes; do not explain every component.

## Recovery Line

> The architecture and pre-seeded workflow are visible. I can walk through the implementation decision and the expected API transition, then follow up with the specific troubleshooting result.
