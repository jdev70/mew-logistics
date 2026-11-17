-- Run locally as SYSDBA against XEPDB1. Do not add passwords to this file.
-- For this first setup, use letters, digits, and underscores in the prompted password.
WHENEVER SQLERROR EXIT FAILURE ROLLBACK

ALTER SESSION SET CONTAINER = XEPDB1;

ACCEPT mew_app_password CHAR PROMPT 'Enter a new local password for MEW_APP (input hidden): ' HIDE

CREATE USER MEW_APP IDENTIFIED BY "&mew_app_password";
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE TRIGGER TO MEW_APP;
ALTER USER MEW_APP QUOTA UNLIMITED ON USERS;

EXIT SUCCESS
