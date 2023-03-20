--
-- create main table that can be used for distributed lock
--
CREATE TABLE lock(lock_name VARCHAR(128) PRIMARY KEY);

--
-- Check all active sessions
--
SELECT
    pid
    ,datname
    ,usename
    ,application_name
    ,client_hostname
    ,client_port
    ,backend_start
    ,query_start
    ,query
    ,state
FROM pg_stat_activity
WHERE datname = 'postgres' AND state = 'active';