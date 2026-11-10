-- 停用导师工作室申请，历史记录保留；重复执行安全。
start transaction;
update gw_studio_application set status='CANCELLED',
 admin_comment=concat(ifnull(admin_comment,''),'；业务调整：固定工位改由管理员直接划分，本申请取消'),update_time=now()
 where status='PENDING';
update sys_menu set menu_name='工作室历史',remark='只读历史申请；固定工位由管理员直接划分' where menu_id=2004;
update sys_menu set status='1' where perms in ('gw:studio:apply','gw:studio:review');
commit;
