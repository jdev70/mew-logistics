set pagesize 0
set feedback off
set heading off
set verify off
set echo off
whenever oserror exit failure
whenever sqlerror exit failure

select 'MAPPING_STATUS=' || case
    when exists (
        select 1 from mew_personnel
        where personnel_id = '&2' and identity_subject = '&1'
    ) then 'MATCH'
    when exists (
        select 1 from mew_personnel
        where personnel_id = '&2'
    ) then 'MISMATCH'
    else 'PERSONNEL_NOT_FOUND'
end
from dual;

select 'ORACLE_SUBJECT=' || nvl(identity_subject, '<UNMAPPED>')
from mew_personnel
where personnel_id = '&2';

exit
