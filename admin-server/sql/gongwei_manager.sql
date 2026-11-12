-- 研究生工位管理业务表
-- 前置：先导入若依官方 sql/ry_20260417.sql，并使用 ry-vue 数据库。
use `ry-vue`;

create table if not exists gw_room (
  room_id bigint not null auto_increment comment '工作室ID',
  room_name varchar(100) not null comment '工作室名称',
  location varchar(255) default '' comment '位置',
  room_type varchar(20) not null default 'STUDIO' comment 'STUDIO/MOBILE',
  mentor_id bigint default null comment '工作室当前独占导师，流动房间为空',
  status varchar(20) not null default 'NORMAL' comment 'NORMAL/MAINTENANCE/DISABLED',
  remark varchar(500) default '' comment '备注',
  create_by varchar(64) default '' comment '创建人',
  create_time datetime default current_timestamp,
  update_by varchar(64) default '' comment '更新人',
  update_time datetime default null,
  primary key (room_id),
  deleted_at datetime null default null comment '房间逻辑删除时间',
  active_room_name varchar(100) generated always as (case when deleted_at is null then room_name else null end) stored,
  unique key uk_gw_room_name (active_room_name),
  key idx_gw_room_type (room_type),
  key idx_gw_room_mentor (mentor_id)
) engine=innodb default charset=utf8mb4 comment='工位房间';

create table if not exists gw_user_profile (
  user_id bigint not null comment '若依用户ID',
  identity_no varchar(64) not null comment '学号/工号',
  user_type varchar(20) not null comment 'STUDENT/MENTOR/INSPECTOR',
  mentor_id bigint default null comment '导师用户ID',
  primary key (user_id),
  unique key uk_gw_identity_no (identity_no),
  key idx_gw_profile_mentor (mentor_id)
) engine=innodb default charset=utf8mb4 comment='工位管理人员扩展资料';

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

create table if not exists gw_workstation (
  workstation_id bigint not null auto_increment comment '工位ID',
  room_id bigint not null comment '工作室ID',
  workstation_no varchar(50) not null comment '房间内编号',
  workstation_type varchar(20) not null comment 'FIXED/MOBILE',
  mentor_id bigint default null comment '固定工位归属导师',
  layout_row int default 0 comment '网格行',
  layout_col int default 0 comment '网格列',
  status varchar(20) not null default 'AVAILABLE' comment 'AVAILABLE/IN_USE/PENDING_RETURN/DISABLED',
  create_by varchar(64) default '' comment '创建人',
  create_time datetime default current_timestamp,
  update_by varchar(64) default '' comment '更新人',
  update_time datetime default null,
  primary key (workstation_id),
  deleted_at datetime null default null comment '逻辑删除时间，历史工位永久保留',
  active_workstation_no varchar(50) generated always as (case when deleted_at is null then workstation_no else null end) stored,
  unique key uk_gw_workstation_no (room_id, active_workstation_no),
  key idx_gw_workstation_room (room_id),
  key idx_gw_workstation_mentor (mentor_id),
  constraint fk_gw_workstation_room foreign key (room_id) references gw_room(room_id)
) engine=innodb default charset=utf8mb4 comment='工位台账';

create table if not exists gw_mobile_application (
  application_id bigint not null auto_increment comment '申请ID',
  student_id bigint not null comment '学生用户ID',
  reason varchar(1000) not null comment '申请原因',
  planned_start date default null comment '拟使用开始日期',
  planned_end date default null comment '拟使用结束日期',
  status varchar(30) not null comment 'PENDING_MENTOR/PENDING_ADMIN/WAIT_ALLOC/COMPLETED/REJECTED',
  mentor_comment varchar(1000) default '' comment '导师意见',
  admin_comment varchar(1000) default '' comment '管理员意见',
  mentor_review_time datetime default null,
  admin_review_time datetime default null,
  workstation_id bigint default null comment '分配工位',
  create_by varchar(64) default '',
  create_time datetime default current_timestamp,
  update_time datetime default null,
  pending_student_id bigint generated always as (case when status in ('PENDING_MENTOR','PENDING_ADMIN','WAIT_ALLOC') then student_id else null end) stored,
  primary key (application_id),
  unique key uk_gw_pending_student (pending_student_id),
  key idx_gw_application_student (student_id),
  key idx_gw_application_status (status)
) engine=innodb default charset=utf8mb4 comment='流动工位申请';

create table if not exists gw_assignment (
  assignment_id bigint not null auto_increment comment '占用记录ID',
  student_id bigint not null comment '学生用户ID',
  workstation_id bigint not null comment '工位ID',
  application_id bigint default null comment '关联流动工位申请',
  term_code varchar(30) default null comment '占用学期',
  started_at datetime not null comment '开始占用时间',
  ended_at datetime default null comment '结束占用时间',
  operator_id bigint not null comment '操作人',
  create_time datetime default current_timestamp,
  active_workstation_id bigint generated always as (case when ended_at is null then workstation_id else null end) stored,
  active_student_id bigint generated always as (case when ended_at is null then student_id else null end) stored,
  primary key (assignment_id),
  unique key uk_gw_active_workstation (active_workstation_id),
  unique key uk_gw_active_student (active_student_id),
  key idx_gw_assignment_student (student_id),
  key idx_gw_assignment_application (application_id)
) engine=innodb default charset=utf8mb4 comment='工位占用历史及当前台账';

alter table gw_assignment add column if not exists term_code varchar(30) default null comment '占用学期' after application_id;

create table if not exists gw_return_application (
  return_id bigint not null auto_increment comment '退还申请ID',
  assignment_id bigint not null comment '占用记录ID',
  student_id bigint not null comment '学生用户ID',
  workstation_id bigint not null comment '工位ID',
  source_type varchar(20) default null comment 'STUDENT/MENTOR/ADMIN；NULL为升级前历史流程',
  status varchar(30) not null default 'PENDING_MENTOR' comment 'PENDING_MENTOR/PENDING_ADMIN/WAIT_CLEAR/WAIT_VERIFY/COMPLETED/REJECTED',
  reason varchar(1000) default '' comment '退还原因',
  mentor_comment varchar(1000) default '',
  admin_comment varchar(1000) default '',
  photo_urls text comment '清空照片地址，逗号分隔1至3张',
  photo_upload_time datetime default null comment '最近上传清空照片时间',
  student_cleared tinyint not null default 0 comment '学生是否确认清空',
  operator_id bigint default null,
  cleared_time datetime default null,
  verified_time datetime default null,
  create_time datetime default current_timestamp,
  update_time datetime default null,
  pending_assignment_id bigint generated always as (case when status in ('PENDING_MENTOR','PENDING_ADMIN','WAIT_CLEAR','WAIT_VERIFY') then assignment_id else null end) stored,
  primary key (return_id),
  unique key uk_gw_return_pending_assignment (pending_assignment_id),
  key idx_gw_return_student (student_id),
  key idx_gw_return_status (status)
) engine=innodb default charset=utf8mb4 comment='工位退还回收申请';

create table if not exists gw_inspection_task (
  task_id bigint not null auto_increment comment '检查任务ID',
  task_name varchar(200) not null comment '任务名称',
  inspector_id bigint not null comment '检查员用户ID',
  room_id bigint default null comment '历史工作室ID；新任务不限定范围',
  scheduled_date date not null comment '计划检查日期',
  status varchar(20) not null default 'PENDING' comment 'PENDING/IN_PROGRESS/SUBMITTED/CLOSED',
  remark varchar(1000) default '',
  create_by varchar(64) default '',
  create_time datetime default current_timestamp,
  update_by varchar(64) default '',
  update_time datetime default null,
  primary key (task_id),
  key idx_gw_inspection_task_inspector (inspector_id),
  key idx_gw_inspection_task_room (room_id),
  key idx_gw_inspection_task_status (status)
) engine=innodb default charset=utf8mb4 comment='卫生检查任务';

create table if not exists gw_inspection_record (
  record_id bigint not null auto_increment comment '检查记录ID',
  task_id bigint not null comment '任务ID',
  inspector_id bigint not null comment '检查员用户ID',
  room_id bigint not null comment '工作室ID',
  workstation_id bigint default null comment '工位ID',
  score int default null comment '卫生评分0-100；历史记录可为空',
  grade char(1) not null comment 'A/B/C/D',
  explanation varchar(2000) default '' comment '评定说明',
  issue_description varchar(2000) default '' comment '问题描述',
  photo_urls text comment '现场照片地址，逗号分隔',
  status varchar(20) not null default 'SUBMITTED' comment 'SUBMITTED/VERIFIED/ARCHIVED',
  admin_comment varchar(1000) default '',
  verified_by bigint default null,
  verified_time datetime default null,
  create_time datetime default current_timestamp,
  update_time datetime default null,
  primary key (record_id),
  unique key uk_gw_inspection_record_target (task_id, workstation_id),
  key idx_gw_inspection_record_inspector (inspector_id),
  key idx_gw_inspection_record_status (status)
) engine=innodb default charset=utf8mb4 comment='卫生检查记录';

create table if not exists gw_audit_log (
  audit_id bigint not null auto_increment,
  business_type varchar(50) not null,
  business_id bigint default null,
  action varchar(50) not null,
  operator_id bigint not null,
  before_data json default null,
  after_data json default null,
  reason varchar(1000) default '',
  create_time datetime default current_timestamp,
  primary key (audit_id),
  key idx_gw_audit_business (business_type, business_id)
) engine=innodb default charset=utf8mb4 comment='工位管理审计日志';

-- 给超级管理员角色挂载工位管理权限；已存在时不重复插入。
insert ignore into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values
(2000, '工位管理', 0, 5, 'workstation', null, 1, 0, 'M', '0', '0', '', 'post', 'admin', now(), '研究生工位管理'),
(2001, '房间管理', 2000, 1, 'room', 'workstation/room/index', 1, 0, 'C', '0', '0', 'gw:room:list', 'tree', 'admin', now(), '工作室与流动工位房间管理'),
(2002, '工位台账', 2000, 2, 'workstation', 'workstation/workstation/index', 1, 0, 'C', '0', '0', 'gw:workstation:list', 'edit', 'admin', now(), '工位台账'),
(2003, '流动工位申请', 2000, 3, 'application', 'workstation/application/index', 1, 0, 'C', '0', '0', 'gw:application:list', 'list', 'admin', now(), '学生流动工位两级审批');

insert ignore into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values
(2010, '工作室查询', 2001, 1, '', null, 1, 0, 'F', '0', '0', 'gw:room:query', '#', 'admin', now(), ''),
(2011, '工作室新增', 2001, 2, '', null, 1, 0, 'F', '0', '0', 'gw:room:add', '#', 'admin', now(), ''),
(2012, '工作室修改', 2001, 3, '', null, 1, 0, 'F', '0', '0', 'gw:room:edit', '#', 'admin', now(), ''),
(2013, '工作室删除', 2001, 4, '', null, 1, 0, 'F', '0', '0', 'gw:room:remove', '#', 'admin', now(), ''),
(2020, '工位查询', 2002, 1, '', null, 1, 0, 'F', '0', '0', 'gw:workstation:query', '#', 'admin', now(), ''),
(2021, '工位新增', 2002, 2, '', null, 1, 0, 'F', '0', '0', 'gw:workstation:add', '#', 'admin', now(), ''),
(2022, '工位修改', 2002, 3, '', null, 1, 0, 'F', '0', '0', 'gw:workstation:edit', '#', 'admin', now(), ''),
(2023, '工位删除', 2002, 4, '', null, 1, 0, 'F', '0', '0', 'gw:workstation:remove', '#', 'admin', now(), ''),
(2030, '申请查询', 2003, 1, '', null, 1, 0, 'F', '0', '0', 'gw:application:query', '#', 'admin', now(), ''),
(2031, '申请提交', 2003, 2, '', null, 1, 0, 'F', '0', '0', 'gw:application:add', '#', 'admin', now(), ''),
(2032, '导师初审', 2003, 3, '', null, 1, 0, 'F', '0', '0', 'gw:application:mentorReview', '#', 'admin', now(), ''),
(2033, '管理员终审', 2003, 4, '', null, 1, 0, 'F', '0', '0', 'gw:application:adminReview', '#', 'admin', now(), ''),
(2034, '工位分配', 2003, 5, '', null, 1, 0, 'F', '0', '0', 'gw:application:assign', '#', 'admin', now(), '');

-- 避免工位管理父路由与工位台账子路由自动生成同名 Workstation，导致前端动态路由注册失败。
update sys_menu set route_name = 'WorkstationManage' where menu_id = 2000;
update sys_menu set route_name = 'WorkstationLedger' where menu_id = 2002;

insert ignore into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values
(2004, '工作室申请', 2000, 4, 'quota', 'workstation/quota/index', 1, 0, 'C', '0', '0', 'gw:studio:list', 'peoples', 'admin', now(), '导师申请独占工作室'),
(2005, '退还回收', 2000, 5, 'return', 'workstation/return/index', 1, 0, 'C', '0', '0', 'gw:return:list', 'delete', 'admin', now(), '工位退还与回收');

insert ignore into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values
(2040, '工作室申请查询', 2004, 1, '', null, 1, 0, 'F', '0', '0', 'gw:studio:list', '#', 'admin', now(), ''),
(2035, '提交工作室申请', 2004, 0, '', null, 1, 0, 'F', '0', '0', 'gw:studio:apply', '#', 'admin', now(), ''),
(2041, '工作室申请审批', 2004, 2, '', null, 1, 0, 'F', '0', '0', 'gw:studio:review', '#', 'admin', now(), ''),
(2042, '解除工作室归属', 2004, 3, '', null, 1, 0, 'F', '0', '0', 'gw:studio:release', '#', 'admin', now(), ''),
(2050, '退还查询', 2005, 1, '', null, 1, 0, 'F', '0', '0', 'gw:return:query', '#', 'admin', now(), ''),
(2051, '退还审核', 2005, 2, '', null, 1, 0, 'F', '0', '0', 'gw:return:review', '#', 'admin', now(), ''),
(2052, '退还核验', 2005, 3, '', null, 1, 0, 'F', '0', '0', 'gw:return:verify', '#', 'admin', now(), '');

insert ignore into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values
(2006, '分配台账', 2000, 6, 'assignment', 'workstation/assignment/index', 1, 0, 'C', '0', '0', 'gw:assignment:list', 'peoples', 'admin', now(), '工位分配与调整'),
(2060, '分配查询', 2006, 1, '', null, 1, 0, 'F', '0', '0', 'gw:assignment:list', '#', 'admin', now(), ''),
(2061, '我的工位', 2006, 2, '', null, 1, 0, 'F', '0', '0', 'gw:assignment:my', '#', 'admin', now(), ''),
(2062, '固定工位分配', 2006, 3, '', null, 1, 0, 'F', '0', '0', 'gw:assignment:assign', '#', 'admin', now(), ''),
(2063, '工位调整', 2006, 4, '', null, 1, 0, 'F', '0', '0', 'gw:assignment:adjust', '#', 'admin', now(), ''),
(2064, '主动收回', 2006, 5, '', null, 1, 0, 'F', '0', '0', 'gw:assignment:recall', '#', 'admin', now(), '');

insert ignore into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values
(2053, '退还申请', 2005, 4, '', null, 1, 0, 'F', '0', '0', 'gw:return:submit', '#', 'admin', now(), ''),
(2054, '上传清空照片', 2005, 5, '', null, 1, 0, 'F', '0', '0', 'gw:return:clear', '#', 'admin', now(), '新流程上传照片；旧数据仍兼容清空确认'),
(2112, '记录上报', 2102, 3, '', null, 1, 0, 'F', '0', '0', 'gw:inspection:record:submit', '#', 'admin', now(), '');

insert ignore into sys_role(role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
values
(3, '导师', 'mentor', 3, '2', 1, 1, '0', '0', 'admin', now(), '研究生导师'),
(4, '卫生检查员', 'inspector', 4, '2', 1, 1, '0', '0', 'admin', now(), '卫生检查员');
insert into sys_role(role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
select '学院管理员','college_admin',5,'1',1,1,'0','0','admin',now(),'工位系统学院管理员'
where not exists(select 1 from sys_role where role_key='college_admin' and del_flag='0');

-- 学生角色沿用若依普通角色，但必须先清除若依示例角色自带的系统管理权限；三类小程序角色统一按白名单授权。
delete from sys_role_menu where role_id in (2, 3, 4) or role_id in (select role_id from sys_role where role_key='college_admin');
insert into sys_role_menu(role_id, menu_id) values
(2, 2001), (2, 2002), (2, 2003), (2, 2030), (2, 2031), (2, 2061), (2, 2050), (2, 2053), (2, 2054),
(3, 2001), (3, 2002), (3, 2003), (3, 2032), (3, 2004), (3, 2035), (3, 2040), (3, 2006), (3, 2060), (3, 2062), (3, 2063), (3, 2064), (3, 2050), (3, 2051),
(4, 2001), (4, 2002), (4, 2100), (4, 2101), (4, 2102), (4, 2110), (4, 2120), (4, 2112);

insert ignore into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values
(2100, '卫生检查', 0, 6, 'inspection', null, 1, 0, 'M', '0', '0', '', 'documentation', 'admin', now(), '卫生检查任务与上报'),
(2101, '检查任务', 2100, 1, 'task', 'inspection/task/index', 1, 0, 'C', '0', '0', 'gw:inspection:task:list', 'form', 'admin', now(), '检查任务管理'),
(2102, '检查记录', 2100, 2, 'record', 'inspection/record/index', 1, 0, 'C', '0', '0', 'gw:inspection:record:list', 'list', 'admin', now(), '卫生检查记录'),
(2200, '师生档案', 0, 7, 'profile', 'workstation/profile/index', 1, 0, 'C', '0', '0', 'gw:profile:list', 'user', 'admin', now(), '师生导师与检查员档案');

insert ignore into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values
(2110, '任务查询', 2101, 1, '', null, 1, 0, 'F', '0', '0', 'gw:inspection:task:query', '#', 'admin', now(), ''),
(2111, '任务新增', 2101, 2, '', null, 1, 0, 'F', '0', '0', 'gw:inspection:task:add', '#', 'admin', now(), ''),
(2120, '记录查询', 2102, 1, '', null, 1, 0, 'F', '0', '0', 'gw:inspection:record:query', '#', 'admin', now(), ''),
(2121, '记录核实', 2102, 2, '', null, 1, 0, 'F', '0', '0', 'gw:inspection:record:verify', '#', 'admin', now(), ''),
(2201, '档案查询', 2200, 1, '', null, 1, 0, 'F', '0', '0', 'gw:profile:list', '#', 'admin', now(), ''),
(2202, '档案维护', 2200, 2, '', null, 1, 0, 'F', '0', '0', 'gw:profile:edit', '#', 'admin', now(), '');

insert ignore into sys_role_menu(role_id, menu_id)
select 1, menu_id from sys_menu where menu_id between 2000 and 2121;
insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id,m.menu_id from sys_role r cross join sys_menu m
where r.role_key='college_admin' and r.del_flag='0'
  and (m.menu_id in (1,100,101,108,500,501,1000,1001,1002,1003,1004,1005,1006,1007,1008,1009,1010,1011,1039,1041,1042,1044,1045)
       or m.menu_id between 2000 and 2202);
delete rm from sys_role_menu rm join sys_role r on r.role_id=rm.role_id where r.role_key='college_admin' and rm.menu_id=2035;

-- 给若依示例普通账号准备最小学生端权限，便于本地联调小程序。
insert ignore into sys_role_menu(role_id, menu_id) values
(2, 2001), (2, 2002), (2, 2003), (2, 2030), (2, 2031), (2, 2061), (2, 2050), (2, 2053), (2, 2054);
insert into gw_user_profile(user_id, identity_no, user_type, mentor_id)
values (2, 'TEST-STUDENT', 'STUDENT', null)
on duplicate key update identity_no = values(identity_no), user_type = values(user_type), mentor_id = values(mentor_id);

-- 开发环境使用账号密码登录；生产环境请按安全策略重新开启验证码。
update sys_config set config_value = 'false', update_by = 'admin', update_time = now()
where config_key = 'sys.account.captchaEnabled';

update sys_config set config_name='用户管理-初始密码强制修改策略',
  remark='0：关闭；1：初始密码验证通过后只允许改密，不签发登录令牌，修改成功后须重新登录'
where config_key='sys.account.initPasswordModify';

-- 开发库中的若依示例账号标记为“首次登录”，登录后必须修改密码。
update sys_user set pwd_update_date = null
where user_name in ('admin', 'ry');

-- 精简若依菜单：保留账号/角色、审计日志及工位业务，底层源码保留便于维护。
update sys_menu set status='1'
where menu_id not in (1,100,101,108,500,501,1000,1001,1002,1003,1004,1005,1006,1007,1008,1009,1010,1011,1039,1040,1041,1042,1043,1044,1045)
  and menu_id not between 2000 and 2202;
update sys_menu set status='0'
where menu_id in (1,100,101,108,500,501,1000,1001,1002,1003,1004,1005,1006,1007,1008,1009,1010,1011,1039,1040,1041,1042,1043,1044,1045)
   or menu_id between 2000 and 2202;


-- 停用导师工作室申请，历史记录保留；重复执行安全。
start transaction;
update gw_studio_application set status='CANCELLED',
 admin_comment=concat(ifnull(admin_comment,''),'；业务调整：固定工位改由管理员直接划分，本申请取消'),update_time=now()
 where status='PENDING';
update sys_menu set menu_name='工作室历史',remark='只读历史申请；固定工位由管理员直接划分' where menu_id=2004;
update sys_menu set status='1' where perms in ('gw:studio:apply','gw:studio:review');
commit;

-- 学生记录、处罚期限、站内消息与申诉
CREATE TABLE IF NOT EXISTS gw_penalty (
  penalty_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT NOT NULL,
  assignment_id BIGINT NOT NULL,
  workstation_id BIGINT NOT NULL,
  room_name VARCHAR(100) NOT NULL,
  workstation_no VARCHAR(50) NOT NULL,
  reason VARCHAR(1000) NOT NULL,
  started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  until_at DATETIME NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  ended_at DATETIME NULL,
  operator_id BIGINT NULL,
  end_reason VARCHAR(1000) NULL,
  active_workstation BIGINT GENERATED ALWAYS AS (CASE WHEN status='ACTIVE' THEN workstation_id ELSE NULL END) STORED,
  UNIQUE KEY uk_penalty_active(active_workstation),
  KEY idx_penalty_student(student_id,penalty_id),
  KEY idx_penalty_due(status,until_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS gw_message (
  message_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  title VARCHAR(100) NOT NULL,
  content VARCHAR(2000) NOT NULL,
  destination VARCHAR(200) NOT NULL,
  event_key VARCHAR(150) NOT NULL,
  read_at DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_message_event(user_id,event_key),
  KEY idx_message_inbox(user_id,message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS gw_appeal (
  appeal_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT NOT NULL,
  mentor_id BIGINT NOT NULL,
  target_type VARCHAR(20) NOT NULL,
  target_id BIGINT NOT NULL,
  original_score INT NULL,
  revised_score INT NULL,
  reason VARCHAR(2000) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING_MENTOR',
  mentor_comment VARCHAR(1000) NULL,
  mentor_reviewed_at DATETIME NULL,
  admin_comment VARCHAR(1000) NULL,
  admin_id BIGINT NULL,
  admin_reviewed_at DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  active_target BIGINT GENERATED ALWAYS AS (CASE WHEN status IN ('PENDING_MENTOR','PENDING_ADMIN') THEN target_id ELSE NULL END) STORED,
  UNIQUE KEY uk_appeal_pending(student_id,target_type,active_target),
  KEY idx_appeal_student(student_id,appeal_id),
  KEY idx_appeal_review(status,mentor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @gw_score_snapshot_ddl=IF(
 (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='gw_inspection_record' AND column_name='student_id')=0,
 'ALTER TABLE gw_inspection_record ADD COLUMN student_id BIGINT NULL, ADD COLUMN assignment_id BIGINT NULL, ADD KEY idx_inspection_student(student_id,record_id)',
 'SELECT 1');
PREPARE gw_score_snapshot_stmt FROM @gw_score_snapshot_ddl;
EXECUTE gw_score_snapshot_stmt;
DEALLOCATE PREPARE gw_score_snapshot_stmt;

UPDATE gw_inspection_record r SET assignment_id=(
 SELECT a.assignment_id FROM gw_assignment a
 WHERE a.workstation_id=r.workstation_id AND a.started_at<r.create_time
   AND (a.ended_at IS NULL OR a.ended_at>r.create_time)
 ORDER BY a.started_at DESC,a.assignment_id DESC LIMIT 1
) WHERE r.student_id IS NULL AND r.assignment_id IS NULL AND r.workstation_id IS NOT NULL;
UPDATE gw_inspection_record r JOIN gw_assignment a ON a.assignment_id=r.assignment_id
 SET r.student_id=a.student_id WHERE r.student_id IS NULL;

INSERT INTO gw_penalty(student_id,assignment_id,workstation_id,room_name,workstation_no,reason,started_at)
SELECT a.student_id,a.assignment_id,w.workstation_id,r.room_name,w.workstation_no,'历史停用记录，请管理员设置截止时间',COALESCE(w.update_time,NOW())
FROM gw_workstation w JOIN gw_room r ON r.room_id=w.room_id
JOIN gw_assignment a ON a.workstation_id=w.workstation_id AND a.ended_at IS NULL
WHERE w.status='DISABLED' AND w.deleted_at IS NULL
 AND NOT EXISTS(SELECT 1 FROM gw_penalty p WHERE p.workstation_id=w.workstation_id AND p.status='ACTIVE');

INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT 2390,'处罚与申诉',2000,20,'lifecycle','workstation/lifecycle/index',1,0,'C','0','0','gw:assignment:list','message','admin',NOW()
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=2390);
INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT role_id,2390 FROM sys_role WHERE role_key IN ('mentor','college_admin','admin') AND del_flag='0';
