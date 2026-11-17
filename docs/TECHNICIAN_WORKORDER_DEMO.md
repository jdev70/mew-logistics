# Technician Work Order Demo

This is a local, interview-ready demonstration of a maintenance action form (MAF) lifecycle. It uses the seeded work order `N21944-CE02-0001`, assigned to `MEW-PER-0002` (Devon Mercer).

## One-time technician identity setup

From the repository root, provision a local Keycloak account for the seeded technician:

```powershell
.\scripts\keycloak\provision-keycloak-local-user.ps1 -Username mew-tech -PersonnelId MEW-PER-0002
```

The script prints the Keycloak subject. Map that value to the technician in Oracle. At the `SQL>` prompt, enter:

```sql
UPDATE mew_personnel
SET identity_subject = '<subject printed by the script>'
WHERE personnel_id = 'MEW-PER-0002';
COMMIT;
```

Confirm the mapping without changing data:

```powershell
.\scripts\keycloak\verify-keycloak-oracle-mapping.ps1 -Username mew-tech -PersonnelId MEW-PER-0002
```

Restart the Java API once after pulling these changes. Its local data initializer assigns the seeded work order and its Navy resource scope to the technician.

## Demonstration flow

1. Start the Java API with the local Oracle and Keycloak profile, then start the Angular client with `npm run start:keycloak` from `frontend`.
2. Sign in as `mew-tech`. In **Supply & Maintenance → Work Orders**, select `N21944-CE02-0001`.
3. Enter a concise maintenance narrative and labor hours, then select **Submit Completed Maintenance for QA**. The MAF becomes `QA_PENDING` and the completion record is retained.
4. Sign out and sign in as `mew-psm`. Select the same work order. The completion record is visible, and the reviewer can select **Accept QA and Close MAF**.
5. The MAF becomes `CLOSED`. Closing remains unavailable until the technician submits completion, so the screen demonstrates the intended lifecycle rather than a generic status toggle.

The accounts are intentionally separate: a shipboard technician may submit only an assigned maintenance action, while the fleet readiness reviewer performs QA acceptance and closure.
