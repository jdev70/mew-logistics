# Synthetic MEW Dataset

The Java backend includes a deterministic, fictional data generator for development and UI demonstrations. It is never active in a normal, local, Oracle, or production profile.

Run it with:

```powershell
cd backend-java
& "$env:USERPROFILE\tools\maven\apache-maven-3.9.9\bin\mvn.cmd" spring-boot:run "-Dspring-boot.run.profiles=synthetic-data"
```

The `synthetic-data` profile uses a disposable in-memory H2 database. It uses real installation names and geographically relevant platform/home-installation relationships across the U.S. Navy, Marine Corps, Air Force, and Army. Personnel, equipment serials, readiness, supply, maintenance, and audit events are demonstration data.

The generator accepts `mew.synthetic.seed`, `mew.synthetic.scenario` (`demo`, `standard`, or `stress`), and `mew.synthetic.history-days`. For example:

```powershell
& "$env:USERPROFILE\tools\maven\apache-maven-3.9.9\bin\mvn.cmd" spring-boot:run "-Dspring-boot.run.profiles=synthetic-data" "-Dspring-boot.run.arguments=--mew.synthetic.seed=42 --mew.synthetic.scenario=demo --mew.synthetic.history-days=30"
```

The records are representative demonstration data only; they contain no real personnel, operational schedules, credentials, or authoritative logistics/technical values.

The default demonstration identity is **CDR Marcus Vance** (`MEW-PER-0001`), a `FLEET_READINESS_ANALYST` with active `TS/SCI`, `USN`, `OPERATIONAL_CMD` RBAC/ABAC attributes and the complete demonstration policy permission set. Its demo subject is `demo:marcus-vance`. In a Keycloak-backed environment, map the real user's immutable JWT `sub` to this personnel assignment; do not replace it with a username or a hard-coded seed value.
