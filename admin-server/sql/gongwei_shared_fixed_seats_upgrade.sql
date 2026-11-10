-- 固定工位按导师归属。执行前备份；本脚本可以重复执行。
set @needs_upgrade=(select count(*)=0 from information_schema.columns where table_schema=database() and table_name='gw_workstation' and column_name='mentor_id');
set @ddl=if(@needs_upgrade,'alter table gw_workstation add column mentor_id bigint null comment ''固定工位归属导师'', add index idx_gw_workstation_mentor (mentor_id)','select 1');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;
update gw_workstation w
join gw_room r on r.room_id=w.room_id
left join gw_assignment a on a.workstation_id=w.workstation_id and a.ended_at is null
left join gw_user_profile p on p.user_id=a.student_id
set w.mentor_id=coalesce(p.mentor_id,r.mentor_id)
where @needs_upgrade and w.workstation_type='FIXED' and w.mentor_id is null;
-- 保留 room.mentor_id 旧列供历史数据溯源；运行时归属仅以工位为准。
