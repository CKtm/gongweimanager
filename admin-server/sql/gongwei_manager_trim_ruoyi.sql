use `ry-vue`;

-- 保留若依安全与账号底座，只在菜单层停用与工位项目无关的模块。
insert into sys_role(role_name,role_key,role_sort,data_scope,menu_check_strictly,dept_check_strictly,status,del_flag,create_by,create_time,remark)
select '学院管理员','college_admin',5,'1',1,1,'0','0','admin',now(),'工位系统学院管理员'
where not exists(select 1 from sys_role where role_key='college_admin' and del_flag='0');

update sys_menu set status='1'
where menu_id not in (1,100,101,108,500,501,1000,1001,1002,1003,1004,1005,1006,1007,1008,1009,1010,1011,1039,1040,1041,1042,1043,1044,1045)
  and menu_id not between 2000 and 2202;
update sys_menu set status='0'
where menu_id in (1,100,101,108,500,501,1000,1001,1002,1003,1004,1005,1006,1007,1008,1009,1010,1011,1039,1040,1041,1042,1043,1044,1045)
   or menu_id between 2000 and 2202;

delete from sys_role_menu where role_id in (select role_id from sys_role where role_key='college_admin');
insert ignore into sys_role_menu(role_id,menu_id)
select r.role_id,m.menu_id from sys_role r cross join sys_menu m
where r.role_key='college_admin' and r.del_flag='0'
  and (m.menu_id in (1,100,101,108,500,501,1000,1001,1002,1003,1004,1005,1006,1007,1008,1009,1010,1011,1039,1041,1042,1044,1045)
       or m.menu_id between 2000 and 2202);

select menu_id,parent_id,menu_name,perms from sys_menu where status='0' order by parent_id,order_num;
