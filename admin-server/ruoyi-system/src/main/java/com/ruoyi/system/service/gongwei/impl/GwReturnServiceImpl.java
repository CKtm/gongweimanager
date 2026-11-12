package com.ruoyi.system.service.gongwei.impl;

import java.util.ArrayList;
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
import com.ruoyi.system.service.gongwei.IGwReturnService;

@Service
public class GwReturnServiceImpl implements IGwReturnService {
    @Autowired private com.ruoyi.system.service.gongwei.GwStudentOpsService lifecycle;
    private static final String SOURCE_STUDENT = "STUDENT";
    private static final String SOURCE_MENTOR = "MENTOR";
    private static final String SOURCE_ADMIN = "ADMIN";

    @Autowired private GwReturnMapper returnMapper;
    @Autowired private GwAssignmentMapper assignmentMapper;
    @Autowired private GwWorkstationMapper workstationMapper;
    @Autowired private GwUserProfileMapper profileMapper;

    public List<GwReturnApplication> selectList(Long studentId, Long mentorId, String status) {
        return returnMapper.selectList(studentId, mentorId, status);
    }

    @Transactional(rollbackFor = Exception.class)
    public int submit(Long assignmentId, Long studentId, String reason, String photoUrls) {
        if (assignmentId == null || reason == null || reason.isBlank()) {
            throw new ServiceException("流动工位和退还原因不能为空");
        }
        String normalizedPhotos = normalizePhotoUrls(photoUrls);
        GwAssignment assignment = assignmentMapper.selectByIdForUpdate(assignmentId);
        if (assignment == null || !studentId.equals(assignment.getStudentId()) || assignment.getEndedAt() != null) {
            throw new ServiceException("只能申请退还本人有效工位");
        }
        if (!"MOBILE".equals(assignment.getWorkstationType())) {
            throw new ServiceException("学生只能主动退还本人的流动工位，固定工位请联系导师处理");
        }
        if (returnMapper.countPendingByAssignment(assignmentId) > 0) {
            throw new ServiceException("该工位已有未完成退还申请");
        }
        if (profileMapper.selectMentorId(studentId) == null) {
            throw new ServiceException("尚未绑定导师，请联系管理员完成师生关系绑定后再申请退还");
        }
        GwWorkstation workstation = workstationMapper.selectWorkstationForUpdate(assignment.getWorkstationId());
        if (workstation == null || !"MOBILE".equals(workstation.getWorkstationType()) || !"IN_USE".equals(workstation.getStatus())) {
            throw new ServiceException("流动工位当前状态已变化，请刷新后重试");
        }
        GwReturnApplication application = new GwReturnApplication();
        application.setAssignmentId(assignmentId);
        application.setStudentId(studentId);
        application.setWorkstationId(assignment.getWorkstationId());
        application.setSourceType(SOURCE_STUDENT);
        application.setReason(reason.trim());
        application.setPhotoUrls(normalizedPhotos);
        application.setStatus("PENDING_MENTOR");
        try {
            if (returnMapper.insertApplication(application) == 0) throw new ServiceException("退还申请创建失败");
        } catch (DataIntegrityViolationException ex) {
            throw new ServiceException("该工位已有未完成退还申请，请刷新后重试");
        }
        if (workstationMapper.updateStatus(assignment.getWorkstationId(), "PENDING_RETURN") == 0) {
            throw new ServiceException("工位待回收状态更新失败");
        }
        returnMapper.insertAudit(application.getReturnId(), "STUDENT_SUBMIT", studentId, null,
            "PENDING_MENTOR", reason.trim());
        lifecycle.workflowChanged(application.getReturnId(), true);
        return 1;
    }

    @Transactional(rollbackFor = Exception.class)
    public int mentorReview(Long returnId, Long mentorId, String status, String comment) {
        validateReview(status, comment, "导师");
        GwReturnApplication item = returnMapper.selectByIdForUpdate(returnId);
        if (item == null || !"PENDING_MENTOR".equals(item.getStatus())
            || !mentorId.equals(profileMapper.selectMentorId(item.getStudentId()))) {
            throw new ServiceException("退还申请不存在、不在待导师审核状态或不属于您的学生");
        }
        if (item.getSourceType() == null) {
            return reviewLegacyByMentor(item, mentorId, status, comment.trim());
        }
        if (!SOURCE_STUDENT.equals(item.getSourceType())) {
            throw new ServiceException("该来源的退还申请不需要导师审核");
        }
        normalizePhotoUrls(item.getPhotoUrls());
        requireActiveAssignment(item);
        GwWorkstation workstation = requirePendingWorkstation(item);
        String toStatus = "APPROVED".equals(status) ? "PENDING_ADMIN" : "REJECTED";
        int rows = returnMapper.updateStatus(returnId, "PENDING_MENTOR", toStatus, comment.trim(), mentorId);
        if (rows == 0) throw new ServiceException("退还申请状态已变化，请刷新后重试");
        if ("REJECTED".equals(toStatus) && workstationMapper.updateStatus(workstation.getWorkstationId(), "IN_USE") == 0) {
            throw new ServiceException("工位状态恢复失败");
        }
        returnMapper.insertAudit(returnId, "MENTOR_" + status, mentorId, "PENDING_MENTOR", toStatus, comment.trim());
        lifecycle.workflowChanged(returnId, true);
        return rows;
    }

    @Transactional(rollbackFor = Exception.class)
    public int adminReview(Long returnId, String status, String comment, Long operatorId) {
        validateReview(status, comment, "管理员");
        GwReturnApplication item = returnMapper.selectByIdForUpdate(returnId);
        if (item == null || !"PENDING_ADMIN".equals(item.getStatus())) {
            throw new ServiceException("退还申请不存在或不在待管理员审核状态");
        }
        if (item.getSourceType() == null) {
            return reviewLegacyByAdmin(item, status, comment.trim(), operatorId);
        }
        if (!isNewPhotoFlow(item.getSourceType())) {
            throw new ServiceException("退还申请来源不合法");
        }
        normalizePhotoUrls(item.getPhotoUrls());
        GwAssignment assignment = requireActiveAssignment(item);
        GwWorkstation workstation = requirePendingWorkstation(item);
        if ("APPROVED".equals(status)) {
            if (returnMapper.completeAfterAdminReview(returnId, comment.trim(), operatorId) == 0) {
                throw new ServiceException("退还申请状态已变化，请刷新后重试");
            }
            if (assignmentMapper.finishAssignment(assignment.getAssignmentId(), operatorId) == 0) {
                throw new ServiceException("占用记录释放失败");
            }
            if (workstationMapper.updateStatus(workstation.getWorkstationId(), "AVAILABLE") == 0) {
                throw new ServiceException("工位释放失败");
            }
            returnMapper.insertAudit(returnId, "ADMIN_APPROVED", operatorId, "PENDING_ADMIN", "COMPLETED", comment.trim());
        lifecycle.workflowChanged(returnId, true);
            assignmentMapper.insertAudit(assignment.getAssignmentId(), "RECLAIM_COMPLETE", operatorId,
                item.getStudentId(), item.getWorkstationId(), null, comment.trim());
            return 1;
        }
        int rows = returnMapper.updateStatus(returnId, "PENDING_ADMIN", "WAIT_CLEAR", comment.trim(), null);
        if (rows == 0) throw new ServiceException("退还申请状态已变化，请刷新后重试");
        returnMapper.insertAudit(returnId, "ADMIN_CLEANING_REJECTED", operatorId,
            "PENDING_ADMIN", "WAIT_CLEAR", comment.trim());
        lifecycle.workflowChanged(returnId, true);
        return rows;
    }

    @Transactional(rollbackFor = Exception.class)
    public int uploadPhotos(Long returnId, Long studentId, String photoUrls) {
        String normalizedPhotos = normalizePhotoUrls(photoUrls);
        GwReturnApplication item = returnMapper.selectByIdForUpdate(returnId);
        if (item == null || !studentId.equals(item.getStudentId()) || !"WAIT_CLEAR".equals(item.getStatus())) {
            throw new ServiceException("只能为本人待清理的工位上传照片");
        }
        if (!isNewPhotoFlow(item.getSourceType())) {
            throw new ServiceException("该记录属于历史清空流程，请使用原清空确认操作");
        }
        requireActiveAssignment(item);
        requirePendingWorkstation(item);
        String toStatus = SOURCE_STUDENT.equals(item.getSourceType()) ? "PENDING_MENTOR" : "PENDING_ADMIN";
        int rows = returnMapper.updatePhotos(returnId, studentId, toStatus, normalizedPhotos);
        if (rows == 0) throw new ServiceException("退还申请状态已变化，请刷新后重试");
        returnMapper.insertAudit(returnId, "PHOTOS_UPLOADED", studentId, "WAIT_CLEAR", toStatus, "学生上传清空照片");
        lifecycle.workflowChanged(returnId, true);
        return rows;
    }

    /** 兼容升级前没有 source_type 的清空确认流程。 */
    @Transactional(rollbackFor = Exception.class)
    public int markCleared(Long returnId, Long studentId) {
        GwReturnApplication item = returnMapper.selectByIdForUpdate(returnId);
        if (item == null || !studentId.equals(item.getStudentId())) throw new ServiceException("无权操作该退还申请");
        if (item.getSourceType() != null) throw new ServiceException("新退还流程必须上传1至3张清空照片");
        int rows = returnMapper.markCleared(returnId);
        if (rows == 0) throw new ServiceException("历史退还申请不在待清空状态");
        returnMapper.insertAudit(returnId, "LEGACY_CLEAR_CONFIRMED", studentId, "WAIT_CLEAR", "WAIT_VERIFY", "历史清空确认");
        lifecycle.workflowChanged(returnId, true);
        return rows;
    }

    /** 兼容升级前没有 source_type 的管理员核验流程。 */
    @Transactional(rollbackFor = Exception.class)
    public void verify(Long returnId, boolean approved, String comment, Long operatorId) {
        if (comment == null || comment.isBlank()) throw new ServiceException("请填写回收核验意见");
        GwReturnApplication item = returnMapper.selectByIdForUpdate(returnId);
        if (item == null || !"WAIT_VERIFY".equals(item.getStatus()) || item.getSourceType() != null) {
            throw new ServiceException("退还申请不属于可核验的历史流程");
        }
        if (!approved) {
            if (returnMapper.resetClearance(returnId, comment.trim(), operatorId) == 0) {
                throw new ServiceException("退还状态已变化，请刷新后重试");
            }
            returnMapper.insertAudit(returnId, "LEGACY_VERIFY_REJECTED", operatorId,
                "WAIT_VERIFY", "WAIT_CLEAR", comment.trim());
        lifecycle.workflowChanged(returnId, true);
            assignmentMapper.insertAudit(item.getAssignmentId(), "CLEAR_REJECTED", operatorId,
                item.getStudentId(), item.getWorkstationId(), item.getWorkstationId(), comment.trim());
            return;
        }
        if (returnMapper.finishReturn(returnId, item.getAssignmentId(), operatorId, comment.trim()) == 0) {
            throw new ServiceException("退还状态更新失败");
        }
        if (assignmentMapper.finishAssignment(item.getAssignmentId(), operatorId) == 0) {
            throw new ServiceException("占用记录释放失败");
        }
        if (workstationMapper.updateStatus(item.getWorkstationId(), "AVAILABLE") == 0) {
            throw new ServiceException("工位释放失败");
        }
        returnMapper.insertAudit(returnId, "LEGACY_VERIFY_APPROVED", operatorId,
            "WAIT_VERIFY", "COMPLETED", comment.trim());
        lifecycle.workflowChanged(returnId, true);
        assignmentMapper.insertAudit(item.getAssignmentId(), "RECLAIM_COMPLETE", operatorId,
            item.getStudentId(), item.getWorkstationId(), null, comment.trim());
    }

    private GwAssignment requireActiveAssignment(GwReturnApplication item) {
        GwAssignment assignment = assignmentMapper.selectByIdForUpdate(item.getAssignmentId());
        if (assignment == null || assignment.getEndedAt() != null
            || !item.getStudentId().equals(assignment.getStudentId())
            || !item.getWorkstationId().equals(assignment.getWorkstationId())) {
            throw new ServiceException("关联占用记录已变化，不能继续处理");
        }
        return assignment;
    }

    private int reviewLegacyByMentor(GwReturnApplication item, Long mentorId, String status, String comment) {
        requireActiveAssignment(item);
        requireInUseWorkstation(item);
        String toStatus = "APPROVED".equals(status) ? "PENDING_ADMIN" : "REJECTED";
        int rows = returnMapper.updateStatus(item.getReturnId(), "PENDING_MENTOR", toStatus, comment, mentorId);
        if (rows == 0) throw new ServiceException("历史退还申请状态已变化，请刷新后重试");
        returnMapper.insertAudit(item.getReturnId(), "LEGACY_MENTOR_" + status, mentorId,
            "PENDING_MENTOR", toStatus, comment);
        lifecycle.workflowChanged(item.getReturnId(), true);
        return rows;
    }

    private int reviewLegacyByAdmin(GwReturnApplication item, String status, String comment, Long operatorId) {
        requireActiveAssignment(item);
        GwWorkstation workstation = requireInUseWorkstation(item);
        String toStatus = "APPROVED".equals(status) ? "WAIT_CLEAR" : "REJECTED";
        int rows = returnMapper.updateStatus(item.getReturnId(), "PENDING_ADMIN", toStatus, comment, null);
        if (rows == 0) throw new ServiceException("历史退还申请状态已变化，请刷新后重试");
        if ("APPROVED".equals(status)
            && workstationMapper.updateStatus(workstation.getWorkstationId(), "PENDING_RETURN") == 0) {
            throw new ServiceException("工位待回收状态更新失败");
        }
        returnMapper.insertAudit(item.getReturnId(), "LEGACY_ADMIN_" + status, operatorId,
            "PENDING_ADMIN", toStatus, comment);
        lifecycle.workflowChanged(item.getReturnId(), true);
        return rows;
    }

    private GwWorkstation requireInUseWorkstation(GwReturnApplication item) {
        GwWorkstation workstation = workstationMapper.selectWorkstationForUpdate(item.getWorkstationId());
        if (workstation == null || !"IN_USE".equals(workstation.getStatus())) {
            throw new ServiceException("历史退还记录关联的工位不在使用中状态");
        }
        return workstation;
    }

    private GwWorkstation requirePendingWorkstation(GwReturnApplication item) {
        GwWorkstation workstation = workstationMapper.selectWorkstationForUpdate(item.getWorkstationId());
        if (workstation == null || !"PENDING_RETURN".equals(workstation.getStatus())) {
            throw new ServiceException("工位不在待回收状态，不能继续处理");
        }
        return workstation;
    }

    private boolean isNewPhotoFlow(String sourceType) {
        return SOURCE_STUDENT.equals(sourceType) || SOURCE_MENTOR.equals(sourceType) || SOURCE_ADMIN.equals(sourceType);
    }

    private void validateReview(String status, String comment, String actor) {
        if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) {
            throw new ServiceException(actor + "审核状态不合法");
        }
        if (comment == null || comment.isBlank()) throw new ServiceException("请填写" + actor + "审核意见");
    }

    private String normalizePhotoUrls(String photoUrls) {
        if (photoUrls == null || photoUrls.isBlank()) throw new ServiceException("请上传1至3张清空照片");
        String[] rawUrls = photoUrls.split(",", -1);
        if (rawUrls.length < 1 || rawUrls.length > 3) throw new ServiceException("清空照片数量必须为1至3张");
        List<String> normalized = new ArrayList<>(rawUrls.length);
        for (String rawUrl : rawUrls) {
            String url = rawUrl == null ? "" : rawUrl.trim();
            if (url.isEmpty()) throw new ServiceException("清空照片地址不能为空");
            normalized.add(url);
        }
        return String.join(",", normalized);
    }
}
