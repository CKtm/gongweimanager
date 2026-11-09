-- 最终业务流程升级：工作室独占、多工作室、真实工位容量、流动工位两级审批。
-- 可在已有 ry-vue 数据库重复执行。
use `ry-vue`;

set @has_room_type := (select count(*) from information_schema.columns where table_schema=database() and table_name='gw_room' and column_name='room_type');
set @ddl := if(@has_room_type=0, 'alter table gw_room add column room_type varchar(20) not null default ''STUDIO'' comment ''STUDIO/MOBILE'' after location', 'select 1');
prepare stmt from @ddl; execute stmt; deallocate prepare stmt;

set @has_room_mentor := (select count(*) from information_schema.columns where table_schema=database() and table_name='gw_room' and column_name='mentor_id');
set @ddl := if(@has_room_mentor=0, 'alter table gw_room add column mentor_id bigint default null comment ''工作室当前独占导师'' after room_type', 'select 1');
prepare stmt from @ddl; execute stmt; deallocate prepare stmt;

set @has_room_type_idx := (select count(*) from information_schema.statistics where table_schema=database() and table_name='gw_room' and index_name='idx_gw_room_type');
set @ddl := if(@has_room_type_idx=0, 'alter table gw_room add index idx_gw_room_type(room_type)', 'select 1');
prepare stmt from @ddl; execute stmt; deallocate prepare stmt;

set @has_room_mentor_idx := (select count(*) from information_schema.statistics where table_schema=database() and table_name='gw_room' and index_name='idx_gw_room_mentor');
set @ddl := if(@has_room_mentor_idx=0, 'alter table gw_room add index idx_gw_room_mentor(mentor_id)', 'select 1');
prepare stmt from @ddl; execute stmt; deallocate prepare stmt;

-- 首次增加字段时：只有全部为流动工位的存量房间迁移为流动房间。
set @migrate_room_type := if(@has_room_type=0,
  'update gw_room r set room_type=case when exists(select 1 from gw_workstation w where w.room_id=r.room_id and w.workstation_type=''MOBILE'') and not exists(select 1 from gw_workstation w where w.room_id=r.room_id and w.workstation_type=''FIXED'') then ''MOBILE'' else ''STUDIO'' end',
  'select 1');
prepare stmt from @migrate_room_type; execute stmt; deallocate prepare stmt;

create table if not exists gw_studio_application (
  studio_application_id bigint not null auto_increment comment '工作室申请ID',
  mentor_id bigint not null comment '申请导师',
  room_id bigint not null comment '申请工作室',
  reason varchar(1000) not null comment '申请理由',
  status varchar(20) not null default 'PENDING' comment 'PENDING/APPROVED/REJECTED/RETURNED',
  admin_comment varchar(1000) default '' comment '审批或解除意见',
  reviewer_id bigint default null,
  reviewed_time datetime default null,
  create_time datetime default current_timestamp,
  update_time datetime default null,
  pending_room_id bigint generated always as (case when status='PENDING' then room_id else null end) stored,
  primary key (studio_application_id),
  unique key uk_gw_studio_pending_room (pending_room_id),
  key idx_gw_studio_mentor (mentor_id),
  key idx_gw_studio_room (room_id),
  key idx_gw_studio_status (status)
) engine=innodb default charset=utf8mb4 comment='导师工作室申请';

update sys_menu set menu_name='房间管理',remark='工作室与流动工位房间管理' where menu_id=2001;
update sys_menu set menu_name='流动工位申请',remark='学生提交、导师初审、管理员终审并分配' where menu_id=2003;
update sys_menu set menu_name='工作室申请',perms='gw:studio:list',remark='导师申请独占工作室' where menu_id=2004;
update sys_menu set menu_name='提交工作室申请',perms='gw:studio:apply' where menu_id=2035;
update sys_menu set menu_name='工作室申请查询',perms='gw:studio:list' where menu_id=2040;
update sys_menu set menu_name='工作室申请审批',perms='gw:studio:review' where menu_id=2041;
update sys_menu set menu_name='解除工作室归属',perms='gw:studio:release' where menu_id=2042;

-- 导师直接分配、调整和收回自己学生的固定工位；彻底下线旧“数字配额/分配建议”菜单入口。
delete from sys_role_menu where menu_id in (2065,2066,2067);
delete from sys_menu where menu_id in (2065,2066,2067);
insert ignore into sys_role_menu(role_id,menu_id)
select r.role_id,m.menu_id from sys_role r join sys_menu m on m.menu_id in (2004,2035,2040,2006,2060,2062,2063,2064)
where r.role_key='mentor' and r.del_flag='0';

-- 学院管理员保留完整项目权限。
delete rm from sys_role_menu rm join sys_role r on r.role_id=rm.role_id where r.role_key='college_admin' and rm.menu_id=2035;
insert ignore into sys_role_menu(role_id,menu_id)
select r.role_id,m.menu_id from sys_role r join sys_menu m on m.menu_id in (2004,2040,2041,2042)
where r.role_key='college_admin' and r.del_flag='0';

update sys_config set config_name='用户管理-初始密码强制修改策略',
  remark='0：关闭；1：初始密码验证通过后只允许改密，不签发登录令牌，修改成功后须重新登录'
where config_key='sys.account.initPasswordModify';
