-- 流动工位照片退还与多来源主动收回增量升级脚本。
-- 可重复执行；只补充字段和菜单文案，不改写任何既有退还记录。
-- 既有记录 source_type 保持 NULL，以继续使用旧 clear/verify 兼容流程。
use `ry-vue`;

set @has_return_source_type := (
  select count(*) from information_schema.columns
  where table_schema=database() and table_name='gw_return_application' and column_name='source_type'
);
set @ddl := if(@has_return_source_type=0,
  'alter table gw_return_application add column source_type varchar(20) default null comment ''STUDENT/MENTOR/ADMIN；NULL为升级前历史流程'' after workstation_id',
  'select 1');
prepare stmt from @ddl; execute stmt; deallocate prepare stmt;

set @has_return_photo_urls := (
  select count(*) from information_schema.columns
  where table_schema=database() and table_name='gw_return_application' and column_name='photo_urls'
);
set @ddl := if(@has_return_photo_urls=0,
  'alter table gw_return_application add column photo_urls text comment ''清空照片地址，逗号分隔1至3张'' after admin_comment',
  'select 1');
prepare stmt from @ddl; execute stmt; deallocate prepare stmt;

set @has_return_photo_upload_time := (
  select count(*) from information_schema.columns
  where table_schema=database() and table_name='gw_return_application' and column_name='photo_upload_time'
);
set @ddl := if(@has_return_photo_upload_time=0,
  'alter table gw_return_application add column photo_upload_time datetime default null comment ''最近上传清空照片时间'' after photo_urls',
  'select 1');
prepare stmt from @ddl; execute stmt; deallocate prepare stmt;

update sys_menu
set menu_name='上传清空照片', remark='新流程上传照片；旧数据仍兼容清空确认'
where menu_id=2054 and perms='gw:return:clear';

