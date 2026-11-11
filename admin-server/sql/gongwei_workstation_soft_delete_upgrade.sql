-- Run against the project database before starting the updated backend. MySQL 8.0.
-- Idempotent: active seats remain unchanged. Deleted records retain their original numbers and IDs.
SET @gw_soft_delete_ddl = IF(
 (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='gw_workstation' AND column_name='deleted_at')=0,
 'ALTER TABLE gw_workstation ADD COLUMN deleted_at datetime NULL DEFAULT NULL COMMENT ''逻辑删除时间'', ADD COLUMN active_workstation_no varchar(50) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN workstation_no ELSE NULL END) STORED, DROP INDEX uk_gw_workstation_no, ADD UNIQUE KEY uk_gw_workstation_no (room_id,active_workstation_no)',
 'SELECT ''workstation soft deletion already installed'' AS migration_status');
PREPARE gw_soft_delete_stmt FROM @gw_soft_delete_ddl;
EXECUTE gw_soft_delete_stmt;
DEALLOCATE PREPARE gw_soft_delete_stmt;
