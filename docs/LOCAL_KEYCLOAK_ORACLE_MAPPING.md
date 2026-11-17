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
