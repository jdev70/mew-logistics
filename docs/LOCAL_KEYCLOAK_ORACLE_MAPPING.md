# Local Keycloak-to-Oracle Identity Mapping

The backend authorizes a verified JWT by matching its immutable `sub` claim to `mew_personnel.identity_subject` in Oracle.

## 1. Obtain the local Keycloak subject

From the repository root, run:

```powershell
.\scripts\keycloak\get-keycloak-subject.ps1
```

It prompts for the local Keycloak administrator password and prints the UUID-like subject for `mew-psm`. The subject is an identifier, not a password.

## 2. Map the subject in Oracle

Open SQL*Plus interactively. It prompts locally for the `MEW_APP` password:

```powershell
& 'C:\app\dataa\product\21c\dbhomeXE\bin\sqlplus.exe' 'MEW_APP@localhost:1521/XEPDB1'
```

At the `SQL>` prompt, replace `<KEYCLOAK_SUBJECT>` with the value from step 1 and run:

```sql
UPDATE mew_personnel
SET identity_subject = '<KEYCLOAK_SUBJECT>'
WHERE personnel_id = 'MEW-PER-0000';

COMMIT;

SELECT personnel_id, identity_subject, title
FROM mew_personnel
WHERE personnel_id = 'MEW-PER-0000';

EXIT;
```

The query should show exactly one `MEW-PER-0000` row with the Keycloak subject value.

## 3. Verify the mapping without changing data

Use the read-only verifier after mapping a user, restarting local services, or before debugging an
authorization failure:

```powershell
.\scripts\keycloak\verify-keycloak-oracle-mapping.ps1
```

It prompts for the local Keycloak administrator password and then lets SQL*Plus securely prompt for
the local `MEW_APP` password. It reports `MAPPING_STATUS=MATCH` only when the live Keycloak user
subject exactly matches the Oracle assignment. It does not update either system.

## Local Keycloak persistence and recovery

The local Docker Compose configuration stores Keycloak state in the named `keycloak-data` Docker
volume. Stopping or recreating the container normally preserves local users and their immutable
subjects, so an existing Oracle mapping remains valid.

### One-time transition to persistent storage

If your currently running Keycloak container was created before this named volume was added, the
first `docker compose up -d` that recreates it initializes a new persistent volume. That is a
one-time local identity reset: provision `mew-psm` again, map its newly printed subject to Oracle,
and run the read-only verifier. Subsequent container restarts and recreations retain that user and
subject. Run the verifier before relying on the local environment after this one-time transition.

Do not remove `keycloak-data` unless you deliberately want to reset local Keycloak. A reset removes
the local Keycloak users and can result in newly created users receiving different subjects. After a
deliberate reset, provision the user again and repeat the Oracle mapping and verification steps above.
