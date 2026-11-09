use `ry-vue`;

-- 演示账号统一初始密码：Demo@123456；数据库只保存 BCrypt 哈希，首次登录强制修改。
insert into sys_role(role_name,role_key,role_sort,data_scope,menu_check_strictly,dept_check_strictly,status,del_flag,create_by,create_time,remark)
select '学院管理员','college_admin',5,'1',1,1,'0','0','admin',now(),'工位系统学院管理员'
where not exists(select 1 from sys_role where role_key='college_admin' and del_flag='0');
insert ignore into sys_role_menu(role_id,menu_id)
select r.role_id,m.menu_id from sys_role r cross join sys_menu m
where r.role_key='college_admin' and r.del_flag='0'
  and (m.menu_id in (1,100,101,108,500,501,1000,1001,1002,1003,1004,1005,1006,1007,1008,1009,1010,1011,1039,1041,1042,1044,1045)
       or m.menu_id between 2000 and 2202);

drop procedure if exists seed_gw_demo_users;
delimiter $$
create procedure seed_gw_demo_users()
begin
  declare i int default 1;
  declare v_dept_id bigint default null;
  declare v_user_id bigint;
  declare v_mentor_id bigint;
  declare v_password varchar(100) default '$2a$10$eKego66xwWGm5PnKECDeE.y1LkRHsFJiGgR7oNO36rmjCvtMTxgia';

  select dept_id into v_dept_id from sys_dept where del_flag='0' and status='0' order by dept_id limit 1;

  set i=1;
  while i<=10 do
    insert into sys_user(dept_id,user_name,nick_name,user_type,email,phonenumber,sex,password,status,del_flag,pwd_update_date,create_by,create_time,remark)
    values(v_dept_id,concat('demo_mentor_',lpad(i,2,'0')),concat('测试导师',lpad(i,2,'0')),'00',concat('demo_mentor_',lpad(i,2,'0'),'@example.test'),concat('139',lpad(i,8,'0')),'2',v_password,'0','0',null,'demo_seed',now(),'工位系统演示导师')
    on duplicate key update nick_name=values(nick_name),dept_id=values(dept_id),status='0',del_flag='0',remark=values(remark);
    select user_id into v_user_id from sys_user where user_name=concat('demo_mentor_',lpad(i,2,'0')) limit 1;
    insert into gw_user_profile(user_id,identity_no,user_type,mentor_id)
    values(v_user_id,concat('T2026',lpad(i,3,'0')),'MENTOR',null)
    on duplicate key update identity_no=values(identity_no),user_type='MENTOR',mentor_id=null;
    set i=i+1;
  end while;

  set i=1;
  while i<=100 do
    insert into sys_user(dept_id,user_name,nick_name,user_type,email,phonenumber,sex,password,status,del_flag,pwd_update_date,create_by,create_time,remark)
    values(v_dept_id,concat('demo_stu_',lpad(i,3,'0')),concat('测试学生',lpad(i,3,'0')),'00',concat('demo_stu_',lpad(i,3,'0'),'@example.test'),concat('138',lpad(i,8,'0')),'2',v_password,'0','0',null,'demo_seed',now(),'工位系统演示学生')
    on duplicate key update nick_name=values(nick_name),dept_id=values(dept_id),status='0',del_flag='0',remark=values(remark);
    select user_id into v_user_id from sys_user where user_name=concat('demo_stu_',lpad(i,3,'0')) limit 1;
    select user_id into v_mentor_id from sys_user where user_name=concat('demo_mentor_',lpad(mod(i-1,10)+1,2,'0')) limit 1;
    insert into gw_user_profile(user_id,identity_no,user_type,mentor_id)
    values(v_user_id,concat('S2026',lpad(i,4,'0')),'STUDENT',v_mentor_id)
    on duplicate key update identity_no=values(identity_no),user_type='STUDENT',mentor_id=values(mentor_id);
    set i=i+1;
  end while;

  set i=1;
  while i<=5 do
    insert into sys_user(dept_id,user_name,nick_name,user_type,email,phonenumber,sex,password,status,del_flag,pwd_update_date,create_by,create_time,remark)
    values(v_dept_id,concat('demo_inspector_',lpad(i,2,'0')),concat('测试检查员',lpad(i,2,'0')),'00',concat('demo_inspector_',lpad(i,2,'0'),'@example.test'),concat('137',lpad(i,8,'0')),'2',v_password,'0','0',null,'demo_seed',now(),'工位系统演示卫生检查员')
    on duplicate key update nick_name=values(nick_name),dept_id=values(dept_id),status='0',del_flag='0',remark=values(remark);
    select user_id into v_user_id from sys_user where user_name=concat('demo_inspector_',lpad(i,2,'0')) limit 1;
    insert into gw_user_profile(user_id,identity_no,user_type,mentor_id)
    values(v_user_id,concat('I2026',lpad(i,3,'0')),'INSPECTOR',null)
    on duplicate key update identity_no=values(identity_no),user_type='INSPECTOR',mentor_id=null;
    set i=i+1;
  end while;

  insert into sys_user(dept_id,user_name,nick_name,user_type,email,phonenumber,sex,password,status,del_flag,pwd_update_date,create_by,create_time,remark)
  values(v_dept_id,'demo_admin_01','测试学院管理员','00','demo_admin_01@example.test','13600000001','2',v_password,'0','0',null,'demo_seed',now(),'工位系统演示管理员')
  on duplicate key update nick_name=values(nick_name),dept_id=values(dept_id),status='0',del_flag='0',remark=values(remark);

  delete ur from sys_user_role ur inner join sys_user u on u.user_id=ur.user_id where left(u.user_name,5)='demo_';
  insert ignore into sys_user_role(user_id,role_id) select u.user_id,r.role_id from sys_user u cross join sys_role r where left(u.user_name,9)='demo_stu_' and r.role_key='common' and r.del_flag='0';
  insert ignore into sys_user_role(user_id,role_id) select u.user_id,r.role_id from sys_user u cross join sys_role r where left(u.user_name,12)='demo_mentor_' and r.role_key='mentor' and r.del_flag='0';
  insert ignore into sys_user_role(user_id,role_id) select u.user_id,r.role_id from sys_user u cross join sys_role r where left(u.user_name,15)='demo_inspector_' and r.role_key='inspector' and r.del_flag='0';
  insert ignore into sys_user_role(user_id,role_id) select u.user_id,r.role_id from sys_user u cross join sys_role r where u.user_name='demo_admin_01' and r.role_key='college_admin' and r.del_flag='0';
end$$
delimiter ;

call seed_gw_demo_users();
drop procedure if exists seed_gw_demo_users;

select r.role_key,count(distinct u.user_id) as account_count
from sys_user u inner join sys_user_role ur on ur.user_id=u.user_id inner join sys_role r on r.role_id=ur.role_id
where left(u.user_name,5)='demo_' and u.del_flag='0'
group by r.role_key order by r.role_key;
select count(*) as bound_demo_students from gw_user_profile p inner join sys_user u on u.user_id=p.user_id where left(u.user_name,9)='demo_stu_' and p.mentor_id is not null;
