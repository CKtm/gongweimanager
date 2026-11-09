-- 卫生检查数值评分与多目标任务流程增量升级脚本。
-- 可重复执行；保留原有 A/B/C/D 等级，并为历史记录补充代表分值。
use `ry-vue`;

set @has_inspection_score := (
  select count(*) from information_schema.columns
  where table_schema=database() and table_name='gw_inspection_record' and column_name='score'
);
set @ddl := if(@has_inspection_score=0,
  'alter table gw_inspection_record add column score int default null comment ''卫生评分0-100；历史记录可为空'' after workstation_id',
  'select 1');
prepare stmt from @ddl; execute stmt; deallocate prepare stmt;

update gw_inspection_record
set score = case upper(grade)
  when 'A' then 90
  when 'B' then 80
  when 'C' then 70
  when 'D' then 50
  else null
end
where score is null;

