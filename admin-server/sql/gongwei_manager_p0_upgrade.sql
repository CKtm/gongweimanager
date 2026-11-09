-- 工位管理第一阶段 P0 增量升级脚本。
-- 仅创建新增业务表、菜单和角色，不修改既有账号密码或系统配置。
use `ry-vue`;

-- 房间按类型管理，工作室可独占绑定导师；容量由真实工位记录统计。
set @has_room_type := (select count(*) from information_schema.columns where table_schema=database() and table_name='gw_room' and column_name='room_type');
set @ddl := if(@has_room_type=0, 'alter table gw_room add column room_type varchar(20) not null default ''STUDIO'' after location', 'select 1');
prepare stmt from @ddl; execute stmt; deallocate prepare stmt;
set @has_room_mentor := (select count(*) from information_schema.columns where table_schema=database() and table_name='gw_room' and column_name='mentor_id');
set @ddl := if(@has_room_mentor=0, 'alter table gw_room add column mentor_id bigint default null after room_type', 'select 1');
prepare stmt from @ddl; execute stmt; deallocate prepare stmt;

create table if not exists gw_studio_application (
  studio_application_id bigint not null auto_increment,mentor_id bigint not null,room_id bigint not null,
  reason varchar(1000) not null,status varchar(20) not null default 'PENDING',admin_comment varchar(1000) default '',
  reviewer_id bigint default null,reviewed_time datetime default null,create_time datetime default current_timestamp,update_time datetime default null,
  pending_room_id bigint generated always as (case when status='PENDING' then room_id else null end) stored,
  primary key(studio_application_id),unique key uk_gw_studio_pending_room(pending_room_id),
  key idx_gw_studio_mentor(mentor_id),key idx_gw_studio_room(room_id),key idx_gw_studio_status(status)
) engine=innodb default charset=utf8mb4;

set @has_term_code := (select count(*) from information_schema.columns where table_schema = database() and table_name = 'gw_assignment' and column_name = 'term_code');
set @alter_sql := if(@has_term_code = 0, 'alter table gw_assignment add column term_code varchar(30) default null comment ''占用学期'' after application_id', 'select 1');
prepare stmt from @alter_sql; execute stmt; deallocate prepare stmt;

create table if not exists gw_return_application (
  return_id bigint not null auto_increment,
  assignment_id bigint not null, student_id bigint not null, workstation_id bigint not null,
  source_type varchar(20) default null comment 'STUDENT/MENTOR/ADMIN；NULL为升级前历史流程',
  status varchar(30) not null default 'PENDING_MENTOR', reason varchar(1000) default '',
  mentor_comment varchar(1000) default '', admin_comment varchar(1000) default '',
  photo_urls text comment '清空照片地址，逗号分隔1至3张', photo_upload_time datetime default null,
  student_cleared tinyint not null default 0, operator_id bigint default null,
  cleared_time datetime default null, verified_time datetime default null,
  create_time datetime default current_timestamp, update_time datetime default null,
  pending_assignment_id bigint generated always as (case when status in ('PENDING_MENTOR','PENDING_ADMIN','WAIT_CLEAR','WAIT_VERIFY') then assignment_id else null end) stored,
  primary key (return_id), unique key uk_gw_return_pending_assignment(pending_assignment_id), key idx_gw_return_student(student_id), key idx_gw_return_status(status)
) engine=innodb default charset=utf8mb4;

create table if not exists gw_inspection_task (
  task_id bigint not null auto_increment, task_name varchar(200) not null, inspector_id bigint not null, room_id bigint default null,
  scheduled_date date not null, status varchar(20) not null default 'PENDING', remark varchar(1000) default '',
  create_by varchar(64) default '', create_time datetime default current_timestamp, update_by varchar(64) default '', update_time datetime default null,
  primary key(task_id), key idx_gw_inspection_task_inspector(inspector_id), key idx_gw_inspection_task_room(room_id), key idx_gw_inspection_task_status(status)
) engine=innodb default charset=utf8mb4;

create table if not exists gw_inspection_record (
  record_id bigint not null auto_increment, task_id bigint not null, inspector_id bigint not null, room_id bigint not null, workstation_id bigint default null,
  score int default null comment '卫生评分0-100；历史记录可为空', grade char(1) not null, explanation varchar(2000) default '', issue_description varchar(2000) default '', photo_urls text,
  status varchar(20) not null default 'SUBMITTED', admin_comment varchar(1000) default '', verified_by bigint default null, verified_time datetime default null,
  create_time datetime default current_timestamp, update_time datetime default null,
  primary key(record_id), unique key uk_gw_inspection_record_target(task_id,workstation_id), key idx_gw_inspection_record_inspector(inspector_id), key idx_gw_inspection_record_status(status)
) engine=innodb default charset=utf8mb4;

insert ignore into sys_menu(menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark) values
(2004,'工作室申请',2000,4,'quota','workstation/quota/index',1,0,'C','0','0','gw:studio:list','peoples','admin',now(),'导师申请独占工作室'),
(2005,'退还回收',2000,5,'return','workstation/return/index',1,0,'C','0','0','gw:return:list','delete','admin',now(),'工位退还与回收'),
(2006,'分配台账',2000,6,'assignment','workstation/assignment/index',1,0,'C','0','0','gw:assignment:list','peoples','admin',now(),'工位分配与调整'),
(2100,'卫生检查',0,6,'inspection',null,1,0,'M','0','0','','documentation','admin',now(),'卫生检查任务与上报'),
(2101,'检查任务',2100,1,'task','inspection/task/index',1,0,'C','0','0','gw:inspection:task:list','form','admin',now(),'检查任务管理'),
(2102,'检查记录',2100,2,'record','inspection/record/index',1,0,'C','0','0','gw:inspection:record:list','list','admin',now(),'卫生检查记录'),
(2200,'师生档案',0,7,'profile','workstation/profile/index',1,0,'C','0','0','gw:profile:list','user','admin',now(),'师生导师与检查员档案');

insert ignore into sys_menu(menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark) values
(2035,'提交工作室申请',2004,0,'',null,1,0,'F','0','0','gw:studio:apply','#','admin',now(),''),(2040,'工作室申请查询',2004,1,'',null,1,0,'F','0','0','gw:studio:list','#','admin',now(),''),(2041,'工作室申请审批',2004,2,'',null,1,0,'F','0','0','gw:studio:review','#','admin',now(),''),(2042,'解除工作室归属',2004,3,'',null,1,0,'F','0','0','gw:studio:release','#','admin',now(),''),
(2050,'退还查询',2005,1,'',null,1,0,'F','0','0','gw:return:query','#','admin',now(),''),(2051,'退还审核',2005,2,'',null,1,0,'F','0','0','gw:return:review','#','admin',now(),''),(2052,'退还核验',2005,3,'',null,1,0,'F','0','0','gw:return:verify','#','admin',now(),''),(2053,'退还申请',2005,4,'',null,1,0,'F','0','0','gw:return:submit','#','admin',now(),''),(2054,'上传清空照片',2005,5,'',null,1,0,'F','0','0','gw:return:clear','#','admin',now(),'新流程上传照片；旧数据仍兼容清空确认'),
(2060,'分配查询',2006,1,'',null,1,0,'F','0','0','gw:assignment:list','#','admin',now(),''),(2061,'我的工位',2006,2,'',null,1,0,'F','0','0','gw:assignment:my','#','admin',now(),''),(2062,'固定工位分配',2006,3,'',null,1,0,'F','0','0','gw:assignment:assign','#','admin',now(),''),(2063,'工位调整',2006,4,'',null,1,0,'F','0','0','gw:assignment:adjust','#','admin',now(),''),(2064,'主动收回',2006,5,'',null,1,0,'F','0','0','gw:assignment:recall','#','admin',now(),''),
(2110,'任务查询',2101,1,'',null,1,0,'F','0','0','gw:inspection:task:query','#','admin',now(),''),(2111,'任务新增',2101,2,'',null,1,0,'F','0','0','gw:inspection:task:add','#','admin',now(),''),(2120,'记录查询',2102,1,'',null,1,0,'F','0','0','gw:inspection:record:query','#','admin',now(),''),(2121,'记录核实',2102,2,'',null,1,0,'F','0','0','gw:inspection:record:verify','#','admin',now(),''),(2112,'记录上报',2102,3,'',null,1,0,'F','0','0','gw:inspection:record:submit','#','admin',now(),'');
insert ignore into sys_menu(menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark) values
(2201,'档案查询',2200,1,'',null,1,0,'F','0','0','gw:profile:list','#','admin',now(),''),(2202,'档案维护',2200,2,'',null,1,0,'F','0','0','gw:profile:edit','#','admin',now(),'');

update sys_menu set route_name='WorkstationManage' where menu_id=2000;
update sys_menu set route_name='WorkstationLedger' where menu_id=2002;
insert ignore into sys_role(role_id,role_name,role_key,role_sort,data_scope,menu_check_strictly,dept_check_strictly,status,del_flag,create_by,create_time,remark) values
(3,'导师','mentor',3,'2',1,1,'0','0','admin',now(),'研究生导师'),(4,'卫生检查员','inspector',4,'2',1,1,'0','0','admin',now(),'卫生检查员');
insert into sys_role(role_name,role_key,role_sort,data_scope,menu_check_strictly,dept_check_strictly,status,del_flag,create_by,create_time,remark)
select '学院管理员','college_admin',5,'1',1,1,'0','0','admin',now(),'工位系统学院管理员'
where not exists(select 1 from sys_role where role_key='college_admin' and del_flag='0');
-- 若依默认 common 角色带有系统管理菜单；本项目将其作为学生角色使用，必须改成业务白名单。
delete from sys_role_menu where role_id in (2,3,4) or role_id in (select role_id from sys_role where role_key='college_admin');
insert into sys_role_menu(role_id,menu_id) values
(2,2001),(2,2002),(2,2003),(2,2030),(2,2031),(2,2061),(2,2050),(2,2053),(2,2054),
(3,2001),(3,2002),(3,2003),(3,2032),(3,2004),(3,2035),(3,2040),(3,2006),(3,2060),(3,2062),(3,2063),(3,2064),(3,2050),(3,2051),
(4,2001),(4,2002),(4,2100),(4,2101),(4,2102),(4,2110),(4,2120),(4,2112);
insert ignore into sys_role_menu(role_id,menu_id)
select r.role_id,m.menu_id from sys_role r cross join sys_menu m
where r.role_key='college_admin' and r.del_flag='0'
  and (m.menu_id in (1,100,101,108,500,501,1000,1001,1002,1003,1004,1005,1006,1007,1008,1009,1010,1011,1039,1041,1042,1044,1045)
       or m.menu_id between 2000 and 2202);
delete rm from sys_role_menu rm join sys_role r on r.role_id=rm.role_id where r.role_key='college_admin' and rm.menu_id=2035;

-- 精简若依菜单，仅保留项目需要的账号/角色、日志审计和工位业务。
update sys_menu set status='1'
where menu_id not in (1,100,101,108,500,501,1000,1001,1002,1003,1004,1005,1006,1007,1008,1009,1010,1011,1039,1040,1041,1042,1043,1044,1045)
  and menu_id not between 2000 and 2202;
update sys_menu set status='0'
where menu_id in (1,100,101,108,500,501,1000,1001,1002,1003,1004,1005,1006,1007,1008,1009,1010,1011,1039,1040,1041,1042,1043,1044,1045)
   or menu_id between 2000 and 2202;

update sys_menu set menu_name='房间管理',remark='工作室与流动工位房间管理' where menu_id=2001;
update sys_menu set menu_name='流动工位申请',remark='学生提交、导师初审、管理员终审并分配' where menu_id=2003;
update sys_menu set menu_name='工作室申请',perms='gw:studio:list',remark='导师申请独占工作室' where menu_id=2004;
update sys_menu set menu_name='提交工作室申请',perms='gw:studio:apply' where menu_id=2035;
update sys_menu set menu_name='工作室申请查询',perms='gw:studio:list' where menu_id=2040;
update sys_menu set menu_name='工作室申请审批',perms='gw:studio:review' where menu_id=2041;
update sys_menu set menu_name='解除工作室归属',perms='gw:studio:release' where menu_id=2042;

update sys_config set config_name='用户管理-初始密码强制修改策略',
  remark='0：关闭；1：初始密码验证通过后只允许改密，不签发登录令牌，修改成功后须重新登录'
where config_key='sys.account.initPasswordModify';
