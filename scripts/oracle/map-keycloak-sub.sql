-- Receives a Keycloak JWT subject and MEW personnel ID as SQL*Plus arguments.
-- Run through scripts/keycloak/map-keycloak-user.ps1; do not enter secrets here.
WHENEVER SQLERROR EXIT FAILURE ROLLBACK

DEFINE keycloak_subject = '&1'
DEFINE personnel_id = '&2'

UPDATE mew_personnel
SET identity_subject = '&keycloak_subject'
WHERE personnel_id = '&personnel_id';

BEGIN
    IF SQL%ROWCOUNT <> 1 THEN
        RAISE_APPLICATION_ERROR(-20001, 'Expected exactly one MEW personnel record to be updated.');
    END IF;
END;
/

COMMIT;

SELECT personnel_id, identity_subject, title
FROM mew_personnel
WHERE personnel_id = '&personnel_id';

EXIT SUCCESS
