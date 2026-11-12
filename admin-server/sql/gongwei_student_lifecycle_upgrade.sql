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
