-- 任务不再预设检查范围，具体对象由检查员提交评分时选择。保留历史任务关联。
use `ry-vue`;
alter table gw_inspection_task modify column room_id bigint default null comment '历史工作室ID；新任务不限定范围';
