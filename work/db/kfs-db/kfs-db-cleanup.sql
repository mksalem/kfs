-- Clean out the KFS Scheduler tables
DELETE FROM QRTZ_BLOB_TRIGGERS
/
DELETE FROM QRTZ_CALENDARS
/
DELETE FROM QRTZ_CRON_TRIGGERS
/
DELETE FROM QRTZ_FIRED_TRIGGERS
/
DELETE FROM QRTZ_JOB_DETAILS
/
DELETE FROM QRTZ_JOB_LISTENERS
/
DELETE FROM QRTZ_PAUSED_TRIGGER_GRPS
/
DELETE FROM QRTZ_SCHEDULER_STATE
/
DELETE FROM QRTZ_SIMPLE_TRIGGERS
/
DELETE FROM QRTZ_TRIGGERS
/
DELETE FROM QRTZ_TRIGGER_LISTENERS
/
COMMIT
/
-- Purge session documents and other temp result tables
DELETE FROM KRNS_SESN_DOC_T
/
DELETE FROM KRNS_LOOKUP_SEL_T
/
DELETE FROM KRNS_LOOKUP_RSLT_T
/
COMMIT
/
