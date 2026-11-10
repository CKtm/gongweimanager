package com.ruoyi.system.service.gongwei.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.gongwei.GwAssignment;
import com.ruoyi.system.domain.gongwei.GwReturnApplication;
import com.ruoyi.system.domain.gongwei.GwWorkstation;
import com.ruoyi.system.mapper.gongwei.GwAssignmentMapper;
import com.ruoyi.system.mapper.gongwei.GwReturnMapper;
import com.ruoyi.system.mapper.gongwei.GwUserProfileMapper;
import com.ruoyi.system.mapper.gongwei.GwWorkstationMapper;
import com.ruoyi.system.service.gongwei.IGwAssignmentService;

@Service
public class GwAssignmentServiceImpl implements IGwAssignmentService {
    @Autowired private GwAssignmentMapper assignmentMapper;
    @Autowired private GwWorkstationMapper workstationMapper;
    @Autowired private GwUserProfileMapper profileMapper;
    @Autowired private GwReturnMapper returnMapper;

    public List<GwAssignment> selectAssignmentList(Long studentId, Long mentorId, Boolean activeOnly) { return assignmentMapper.selectAssignmentList(studentId, mentorId, activeOnly); }

    @Transactional(rollbackFor = Exception.class)
    public void assignFixed(Long studentId, Long workstationId, Long operatorId, String termCode, boolean admin) {
        if (studentId == null || workstationId == null || termCode == null || termCode.isBlank()) throw new ServiceException("学生、工位和使用期限不能为空");
        if (termCode.length() > 30) throw new ServiceException("使用期限过长");
        if (termCode.contains("~")) {
            String[] dates = termCode.split("~", -1);
            try {
                if (dates.length != 2 || !dates[0].matches("[0-9]{4}-[0-9]{2}-[0-9]{2}") || !dates[1].matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) throw new IllegalArgumentException();
                java.time.LocalDate start = java.time.LocalDate.parse(dates[0]);
                java.time.LocalDate end = java.time.LocalDate.parse(dates[1]);
                if (end.isBefore(start)) throw new IllegalArgumentException();
            } catch (IllegalArgumentException | java.time.format.DateTimeParseException ex) {
                throw new ServiceException("请选择有效日期，结束日期不能早于开始日期");
            }
        }
        if (assignmentMapper.selectActiveByStudent(studentId) != null) throw new ServiceException("该学生已有有效工位");
        GwWorkstation workstation = workstationMapper.selectWorkstationForUpdate(workstationId);
        if (workstation == null || !"AVAILABLE".equals(workstation.getStatus()) || !"FIXED".equals(workstation.getWorkstationType())
            || !"NORMAL".equals(workstation.getRoomStatus())) throw new ServiceException("目标工位或所属房间不是可用固定工位");
        Long mentorId = profileMapper.selectMentorId(studentId);
        if (mentorId == null) throw new ServiceException("学生尚未绑定导师");
        if (!admin && !mentorId.equals(operatorId)) throw new ServiceException("导师只能给自己的学生分配固定工位");
        if (!"STUDIO".equals(workstation.getRoomType()) || workstation.getMentorId() == null || !mentorId.equals(workstation.getMentorId())) {
            throw new ServiceException("目标固定工位未划归该学生的导师");
        }
        try { assignmentMapper.insertAssignment(studentId, workstationId, null, termCode, operatorId); }
        catch (DataIntegrityViolationException ex) { throw new ServiceException("工位或学生已被分配，请刷新后重试"); }
        if (workstationMapper.updateStatus(workstationId, "IN_USE") == 0) throw new ServiceException("工位状态更新失败");
        GwAssignment assignment = assignmentMapper.selectActiveByStudent(studentId);
        assignmentMapper.insertAudit(assignment == null ? null : assignment.getAssignmentId(), "FIXED_ASSIGN", operatorId, studentId, null, workstationId, "使用期限 " + termCode + " 分配固定工位");
    }

    @Transactional(rollbackFor = Exception.class)
    public void adjustAssignment(Long assignmentId, Long targetWorkstationId, String reason, Long operatorId, boolean admin) {
        if (assignmentId == null || targetWorkstationId == null || reason == null || reason.isBlank()) throw new ServiceException("原占用记录、目标工位和调整原因不能为空");
        GwAssignment current = assignmentMapper.selectByIdForUpdate(assignmentId);
        if (current == null || current.getEndedAt() != null) throw new ServiceException("原工位占用记录不存在或已经结束");
        if (current.getWorkstationId().equals(targetWorkstationId)) throw new ServiceException("目标工位不能与原工位相同");
        if (returnMapper.countPendingByAssignment(assignmentId) > 0) throw new ServiceException("该工位存在未完成退还/回收流程，不能调整");
        GwWorkstation target = workstationMapper.selectWorkstationForUpdate(targetWorkstationId);
        if (target == null || !"AVAILABLE".equals(target.getStatus()) || !"NORMAL".equals(target.getRoomStatus())) throw new ServiceException("目标工位不存在或所属房间不可用");
        if (!current.getWorkstationType().equals(target.getWorkstationType())) throw new ServiceException("调整前后工位类型必须一致");
        Long mentorId = profileMapper.selectMentorId(current.getStudentId());
        if ("FIXED".equals(current.getWorkstationType())) {
            GwWorkstation source = workstationMapper.selectWorkstationForUpdate(current.getWorkstationId());
            if (!admin && (source == null || !operatorId.equals(source.getMentorId()))) throw new ServiceException("导师只能调整自己名下的固定工位");
            if (mentorId == null || target.getMentorId() == null || !mentorId.equals(target.getMentorId())) throw new ServiceException("目标固定工位未划归该学生的导师");
            if (!admin && !mentorId.equals(operatorId)) throw new ServiceException("导师只能调整自己学生的固定工位");
        } else if (!admin) {
            throw new ServiceException("流动工位调整只能由学院管理员操作");
        }
        if (assignmentMapper.finishAssignment(assignmentId, operatorId) == 0) throw new ServiceException("原占用记录释放失败");
        if (workstationMapper.updateStatus(current.getWorkstationId(), "AVAILABLE") == 0) throw new ServiceException("原工位释放失败");
        try { assignmentMapper.insertAssignment(current.getStudentId(), targetWorkstationId, current.getApplicationId(), current.getTermCode(), operatorId); }
        catch (DataIntegrityViolationException ex) { throw new ServiceException("目标工位已被占用，请刷新后重试"); }
        if (workstationMapper.updateStatus(targetWorkstationId, "IN_USE") == 0) throw new ServiceException("目标工位状态更新失败");
        assignmentMapper.insertAudit(assignmentId, "ADJUST", operatorId, current.getStudentId(), current.getWorkstationId(), targetWorkstationId, reason.trim());
    }

    @Transactional(rollbackFor = Exception.class)
    public void initiateRecall(Long assignmentId, String reason, Long operatorId, boolean admin) {
        if (assignmentId == null || reason == null || reason.isBlank()) throw new ServiceException("占用记录和收回原因不能为空");
        GwAssignment current = assignmentMapper.selectByIdForUpdate(assignmentId);
        if (current == null || current.getEndedAt() != null) throw new ServiceException("工位占用记录不存在或已经结束");
        if (returnMapper.countPendingByAssignment(assignmentId) > 0) throw new ServiceException("该工位已有未完成退还/回收流程");
        GwWorkstation currentWorkstation = workstationMapper.selectWorkstationForUpdate(current.getWorkstationId());
        if (currentWorkstation == null || !"IN_USE".equals(currentWorkstation.getStatus())) {
            throw new ServiceException("工位当前不是使用中状态，不能主动收回");
        }
        if (!admin) {
            Long mentorId = profileMapper.selectMentorId(current.getStudentId());
            if (mentorId == null || !mentorId.equals(operatorId)) {
                throw new ServiceException("导师只能收回自己学生的工位");
            }
            if ("FIXED".equals(current.getWorkstationType())) {
                if (!mentorId.equals(currentWorkstation.getMentorId())) {
                    throw new ServiceException("导师只能直接收回自己名下的固定工位");
                }
                if (assignmentMapper.finishAssignment(assignmentId, operatorId) == 0) {
                    throw new ServiceException("占用记录释放失败");
                }
                if (workstationMapper.updateStatus(current.getWorkstationId(), "AVAILABLE") == 0) {
                    throw new ServiceException("工位释放失败");
                }
                assignmentMapper.insertAudit(assignmentId, "MENTOR_RECALL_COMPLETE", operatorId,
                    current.getStudentId(), current.getWorkstationId(), null, reason.trim());
                return;
            }
            if (!"MOBILE".equals(current.getWorkstationType())) {
                throw new ServiceException("导师只能收回固定工位或流动工位");
            }
        }
        GwReturnApplication application = new GwReturnApplication();
        application.setAssignmentId(assignmentId);
        application.setStudentId(current.getStudentId());
        application.setWorkstationId(current.getWorkstationId());
        application.setSourceType(admin ? "ADMIN" : "MENTOR");
        application.setReason((admin ? "管理员" : "导师") + "主动收回：" + reason.trim());
        application.setStatus("WAIT_CLEAR");
        try {
            if (returnMapper.insertApplication(application) == 0) throw new ServiceException("创建收回流程失败");
        } catch (DataIntegrityViolationException ex) {
            throw new ServiceException("该工位已有未完成退还/回收流程，请刷新后重试");
        }
        if (workstationMapper.updateStatus(current.getWorkstationId(), "PENDING_RETURN") == 0) throw new ServiceException("工位待回收状态更新失败");
        String action = admin ? "ADMIN_RECALL" : "MENTOR_MOBILE_RECALL";
        assignmentMapper.insertAudit(assignmentId, action, operatorId, current.getStudentId(), current.getWorkstationId(), null, reason.trim());
        returnMapper.insertAudit(application.getReturnId(), action, operatorId, null, "WAIT_CLEAR", reason.trim());
    }

    public int finishAssignment(Long assignmentId, Long operatorId) { return assignmentMapper.finishAssignment(assignmentId, operatorId); }
}
