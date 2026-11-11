-- MySQL 8.0. Run before starting the updated backend; does not delete existing rooms.
SET @gw_room_delete_ddl = IF(
 (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='gw_room' AND column_name='deleted_at')=0,
 'ALTER TABLE gw_room ADD COLUMN deleted_at datetime NULL DEFAULT NULL COMMENT ''房间逻辑删除时间'', ADD COLUMN active_room_name varchar(100) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN room_name ELSE NULL END) STORED, DROP INDEX uk_gw_room_name, ADD UNIQUE KEY uk_gw_room_name(active_room_name)',
 'SELECT ''room soft deletion already installed'' AS migration_status');
PREPARE gw_room_delete_stmt FROM @gw_room_delete_ddl;
EXECUTE gw_room_delete_stmt;
DEALLOCATE PREPARE gw_room_delete_stmt;
