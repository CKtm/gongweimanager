package com.ruoyi.system.service.gongwei;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.gongwei.GwWorkstation;
import com.ruoyi.system.mapper.gongwei.GwStudentOpsMapper;
import com.ruoyi.system.mapper.gongwei.GwUserProfileMapper;
import com.ruoyi.system.mapper.gongwei.GwWorkstationMapper;

@Service
public class GwStudentOpsService {
    @Autowired private GwStudentOpsMapper mapper;
    @Autowired private GwUserProfileMapper profiles;
    @Autowired private GwWorkstationMapper workstations;

    public static Long number(Map<String, Object> row, String field) {
        Object value = row.get(field);
        return value == null ? null : ((Number) value).longValue();
    }

    public void message(Long userId, String title, String content, String destination, String eventKey) {
        if (userId == null) return;
        Map<String, Object> row = new HashMap<>();
        row.put("userId", userId);
        row.put("title", title);
        row.put("content", content);
        row.put("destination", destination);
        row.put("eventKey", eventKey);
        mapper.insertMessage(row);
    }

    public void notifyAdministrators(String title, String content, String destination, String eventKey) {
        for (Long id : mapper.administratorIds()) message(id, title, content, destination, eventKey);
    }

    public void validateDeadline(Date until, String reason) {
        if (until == null || !until.after(new Date())) throw new ServiceException("请选择晚于当前时间的处罚截止时间");
        requireText(reason, 1000, "停用原因");
    }

    public void startPenalty(GwWorkstation seat, Date until, String reason, Long operatorId) {
        validateDeadline(until, reason);
        Map<String, Object> assignment = mapper.activeAssignment(seat.getWorkstationId());
        if (assignment == null || !Objects.equals(number(assignment, "studentId"), seat.getStudentId())) {
            throw new ServiceException("学生占用记录已变化，请刷新后重试");
        }
        Map<String, Object> row = new HashMap<>();
        row.put("studentId", seat.getStudentId());
        row.put("assignmentId", number(assignment, "assignmentId"));
        row.put("workstationId", seat.getWorkstationId());
        row.put("roomName", seat.getRoomName());
        row.put("workstationNo", seat.getWorkstationNo());
        row.put("reason", reason.trim());
        row.put("untilAt", until);
        row.put("operatorId", operatorId);
        try {
            mapper.insertPenalty(row);
        } catch (DataIntegrityViolationException exception) {
            throw new ServiceException("工位已有生效中的处罚，请刷新后重试");
        }
        message(seat.getStudentId(), "工位已停用", "停用原因：" + reason.trim() + "。请查看处罚记录中的截止时间。",
            "/pages/student-records/student-records?tab=penalties", "PENALTY_START_" + row.get("penaltyId"));
    }

    public void finishPenalty(Long workstationId, String status, String reason) {
        Map<String, Object> row = mapper.activePenalty(workstationId);
        if (row == null) return;
        Long id = number(row, "penaltyId");
        Map<String, Object> penalty = mapper.penaltyForUpdate(id);
        if (mapper.closePenalty(id, status, reason) != 1) throw new ServiceException("处罚状态已变化，请刷新后重试");
        message(number(penalty, "studentId"), "工位处罚状态更新", reason,
            "/pages/student-records/student-records?tab=penalties", "PENALTY_END_" + id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateDeadline(Long id, Date until) {
        Map<String, Object> row = mapper.penalty(id);
        if (row == null) throw new ServiceException("处罚不存在");
        GwWorkstation seat = workstations.selectWorkstationForUpdate(number(row, "workstationId"));
        row = mapper.penaltyForUpdate(id);
        if (seat == null || !"ACTIVE".equals(row.get("status")) || !"DISABLED".equals(seat.getStatus())) {
            throw new ServiceException("只能调整生效中的停用处罚");
        }
        validateDeadline(until, String.valueOf(row.get("reason")));
        if (mapper.updateDeadline(id, until) != 1) throw new ServiceException("处罚状态已变化");
        message(number(row, "studentId"), "处罚截止时间已调整", "管理员调整了处罚截止时间，请查看处罚记录。",
            "/pages/student-records/student-records?tab=penalties", "PENALTY_DEADLINE_" + id + "_" + until.getTime());
    }

    @Transactional(rollbackFor = Exception.class)
    public void expirePenalty(Long id) {
        Map<String, Object> row = mapper.penalty(id);
        if (row == null) return;
        GwWorkstation seat = workstations.selectWorkstationForUpdate(number(row, "workstationId"));
        row = mapper.penaltyForUpdate(id);
        Date until = (Date) row.get("untilAt");
        if (!"ACTIVE".equals(row.get("status")) || until == null || until.after(new Date())) return;
        Map<String, Object> assignment = mapper.activeAssignment(number(row, "workstationId"));
        if (seat == null || assignment == null || !Objects.equals(number(row, "assignmentId"), number(assignment, "assignmentId"))
                || !Objects.equals(number(row, "studentId"), seat.getStudentId())) {
            mapper.closePenalty(id, "RECALLED", "原工位占用已结束，处罚终结");
            return;
        }
        if (!"DISABLED".equals(seat.getStatus())) {
            mapper.closePenalty(id, "RESTORED", "工位已恢复或进入收回流程");
            return;
        }
        if (!"NORMAL".equals(seat.getRoomStatus())) {
            notifyAdministrators("处罚到期恢复待处理", "处罚 #" + id + " 已到期，但房间不可用，请处理房间状态。",
                "/pages/student-records/student-records?tab=penalties", "PENALTY_BLOCKED_" + id);
            return;
        }
        if (workstations.updateStatus(seat.getWorkstationId(), "IN_USE") != 1) throw new ServiceException("恢复工位失败");
        finishPenalty(seat.getWorkstationId(), "EXPIRED", "停用处罚已到期，已自动恢复原工位使用权");
    }

    @Transactional(rollbackFor = Exception.class)
    public void submitAppeal(Long studentId, String type, Long targetId, String reason) {
        requireText(reason, 2000, "申诉原因");
        Long mentorId = profiles.selectMentorId(studentId);
        if (profiles.countActiveUserByRole(studentId, "common") == 0 || mentorId == null
                || profiles.countActiveUserByRole(mentorId, "mentor") == 0) throw new ServiceException("请先绑定有效导师后再申诉");
        Map<String, Object> target;
        if ("SCORE".equals(type)) {
            target = mapper.scoreForUpdate(targetId);
            if (target == null || !List.of("VERIFIED", "ARCHIVED").contains(target.get("status"))) throw new ServiceException("评分尚未核实或不存在");
        } else if ("PENALTY".equals(type)) {
            target = mapper.penaltyForUpdate(targetId);
        } else {
            throw new ServiceException("不支持的申诉类型");
        }
        if (target == null || !studentId.equals(number(target, "studentId"))) throw new ServiceException("只能对本人的评分或处罚申诉");
        Map<String, Object> appeal = new HashMap<>();
        appeal.put("studentId", studentId);
        appeal.put("mentorId", mentorId);
        appeal.put("targetType", type);
        appeal.put("targetId", targetId);
        appeal.put("reason", reason.trim());
        appeal.put("originalScore", target.get("score"));
        try {
            mapper.insertAppeal(appeal);
        } catch (DataIntegrityViolationException exception) {
            throw new ServiceException("该记录已有正在处理的申诉，请勿重复提交");
        }
        message(mentorId, "学生申诉待初审", "收到一条学生申诉，请进入申诉中心处理。",
            "/pages/student-records/student-records?tab=appeals", "APPEAL_SUBMIT_" + appeal.get("appealId"));
    }

    @Transactional(rollbackFor = Exception.class)
    public void reviewAppeal(Long id, Long operatorId, boolean admin, String decision, String comment, Integer revisedScore) {
        requireText(comment, 1000, "审核意见");
        if (decision == null || !List.of("APPROVED", "REJECTED").contains(decision)) throw new ServiceException("审核结果不合法");
        Map<String, Object> appeal = mapper.appealForUpdate(id);
        if (appeal == null) throw new ServiceException("申诉不存在");
        Long studentId = number(appeal, "studentId");
        appeal.put("comment", comment.trim());
        if (!admin) {
            if (!"PENDING_MENTOR".equals(appeal.get("status")) || !operatorId.equals(profiles.selectMentorId(studentId))) {
                throw new ServiceException("申诉不在初审阶段或不属于您的学生");
            }
            appeal.put("status", "APPROVED".equals(decision) ? "PENDING_ADMIN" : "REJECTED");
            if (mapper.mentorReview(appeal) != 1) throw new ServiceException("申诉状态已变化");
            if ("APPROVED".equals(decision)) notifyAdministrators("学生申诉待终审", "导师已通过申诉初审，请处理终审。",
                "/pages/student-records/student-records?tab=appeals", "APPEAL_ADMIN_" + id);
        } else {
            if (!"PENDING_ADMIN".equals(appeal.get("status"))) throw new ServiceException("请先由导师完成初审");
            if ("APPROVED".equals(decision)) resolveAppeal(appeal, revisedScore, comment.trim());
            appeal.put("status", decision);
            appeal.put("operatorId", operatorId);
            appeal.put("revisedScore", "APPROVED".equals(decision) && "SCORE".equals(appeal.get("targetType")) ? revisedScore : null);
            if (mapper.adminReview(appeal) != 1) throw new ServiceException("申诉状态已变化");
        }
        message(studentId, "申诉审核进度更新", (admin ? "管理员终审：" : "导师初审：") + ("APPROVED".equals(decision) ? "通过" : "驳回") + "。" + comment.trim(),
            "/pages/student-records/student-records?tab=appeals", "APPEAL_REVIEW_" + id + "_" + (admin ? "ADMIN" : "MENTOR"));
    }

    private void resolveAppeal(Map<String, Object> appeal, Integer score, String comment) {
        Long targetId = number(appeal, "targetId");
        if ("SCORE".equals(appeal.get("targetType"))) {
            if (score == null || score < 0 || score > 100) throw new ServiceException("评分申诉通过时请填写0至100的调整后分数");
            Map<String, Object> target = mapper.scoreForUpdate(targetId);
            if (target == null || !Objects.equals(number(target, "studentId"), number(appeal, "studentId"))) throw new ServiceException("申诉对象已变化");
            String grade = score >= 90 ? "A" : score >= 80 ? "B" : score >= 60 ? "C" : "D";
            if (mapper.updateScore(targetId, score, grade, "申诉 #" + appeal.get("appealId") + " 调整：" + comment) != 1) throw new ServiceException("评分调整失败");
        } else {
            Map<String, Object> penalty = mapper.penalty(targetId);
            if (penalty == null || !Objects.equals(number(penalty, "studentId"), number(appeal, "studentId"))) throw new ServiceException("处罚不存在");
            Long workstationId = number(penalty, "workstationId");
            GwWorkstation seat = workstations.selectWorkstationForUpdate(workstationId);
            penalty = mapper.penaltyForUpdate(targetId);
            if (!"ACTIVE".equals(penalty.get("status"))) return;
            Map<String, Object> active = mapper.activeAssignment(workstationId);
            if (seat != null && "DISABLED".equals(seat.getStatus()) && active != null
                    && Objects.equals(number(active, "assignmentId"), number(penalty, "assignmentId"))) {
                if (!"NORMAL".equals(seat.getRoomStatus())) throw new ServiceException("房间不可用，请先处理房间状态再恢复");
                if (workstations.updateStatus(workstationId, "IN_USE") != 1) throw new ServiceException("恢复工位失败");
            }
            mapper.closePenalty(targetId, "APPEAL_APPROVED", "申诉通过：" + comment);
        }
    }

    public void scorePublished(Long id) {
        Map<String, Object> score = mapper.scoreForUpdate(id);
        if (score != null) message(number(score, "studentId"), "卫生评分已核实", "你有新的卫生评分，请查看评分记录。",
            "/pages/student-records/student-records?tab=scores", "SCORE_VERIFIED_" + id);
    }

    public void workflowChanged(Long id, boolean returning) {
        Map<String, Object> row = returning ? mapper.returnApplication(id) : mapper.application(id);
        if (row == null) return;
        String title = returning ? "工位退还进度更新" : "流动工位申请进度更新";
        String status = String.valueOf(row.get("status"));
        String destination = returning ? "/pages/returns/returns" : "/pages/applications/applications";
        String key = (returning ? "RETURN_" : "APPLICATION_") + id + "_" + status;
        Long studentId = number(row, "studentId");
        message(studentId, title, "业务 #" + id + " 状态已更新为 " + statusLabel(status) + "，请查看详情。", destination, key);
        if ("PENDING_MENTOR".equals(status)) message(profiles.selectMentorId(studentId), title, "有新的学生业务待审核。", "/pages/mentor/mentor?tab=review", key);
        if ("PENDING_ADMIN".equals(status) || "WAIT_ALLOC".equals(status)) notifyAdministrators(title, "有新的业务待处理。", "/pages/admin/admin?tab=reviews", key);
    }

    private String statusLabel(String status) {
        return switch(status) {
            case "PENDING_MENTOR" -> "待导师审核"; case "PENDING_ADMIN" -> "待管理员审核";
            case "WAIT_ALLOC" -> "待分配"; case "COMPLETED" -> "已完成";
            case "REJECTED" -> "已驳回"; case "WAIT_CLEAR" -> "等待上传清理照片";
            case "WAIT_VERIFY" -> "待核验"; case "APPROVED" -> "已通过";
            case "CANCELLED" -> "已取消"; default -> status;
        };
    }

    private void requireText(String value, int maximum, String label) {
        if (value == null || value.isBlank() || value.trim().length() > maximum) throw new ServiceException(label + "不能为空且不能超过" + maximum + "字");
    }
}
