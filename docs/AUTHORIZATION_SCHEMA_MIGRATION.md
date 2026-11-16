# Authorization Schema Migration

The Java backend resolves the verified JWT subject (`sub`) to `mew_personnel.identity_subject`. Authorization is then read from that personnel record rather than accepted from request headers or mutable token claims.

## Required columns

For an existing Oracle deployment using the `jndi` profile, apply an equivalent reviewed database migration before deploying this code. Hibernate is configured to validate the JNDI schema and will not create these columns.

```sql
ALTER TABLE mew_personnel ADD (
    identity_subject VARCHAR2(150),
    authorized_installations VARCHAR2(500) DEFAULT '[]' NOT NULL,
    service_branch VARCHAR2(10) DEFAULT 'USN' NOT NULL,
    command_tier VARCHAR2(30) DEFAULT 'INSTALLATION_UNIT' NOT NULL,
    authorization_permissions VARCHAR2(2000) DEFAULT '' NOT NULL
);

CREATE UNIQUE INDEX ux_mew_personnel_identity_subject
    ON mew_personnel (identity_subject);

ALTER TABLE mew_installation ADD (
    service_branch VARCHAR2(10) DEFAULT 'JOINT' NOT NULL
);
```

Backfill `identity_subject` from the identity provider's immutable subject, then assign a valid `MilitaryBranch`, `CommandTier`, `ClearanceLevel`, installation/CAGE scope, and comma-delimited `Permission` values. Do not use `ALL` outside local seed data.

Only make `identity_subject` non-null after every authorized personnel record has been backfilled.
