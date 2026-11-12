package com.ruoyi.system.service.gongwei.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.gongwei.GwMobileApplication;
import com.ruoyi.system.domain.gongwei.GwWorkstation;
import com.ruoyi.system.mapper.gongwei.GwApplicationMapper;
import com.ruoyi.system.mapper.gongwei.GwUserProfileMapper;
import com.ruoyi.system.mapper.gongwei.GwWorkstationMapper;
import com.ruoyi.system.service.gongwei.IGwApplicationService;

@Service
public class GwApplicationServiceImpl implements IGwApplicationService {
    @Autowired
    private GwApplicationMapper applicationMapper;
    @Autowired private com.ruoyi.system.service.gongwei.GwStudentOpsService lifecycle;

    @Autowired
    private GwWorkstationMapper workstationMapper;

    @Autowired
    private GwUserProfileMapper profileMapper;

    @Override
    public List<GwMobileApplication> selectApplicationList(GwMobileApplication application, String scope, Long userId) {
        return applicationMapper.selectApplicationList(application, scope, userId);
    }

    @Override
    public GwMobileApplication selectApplicationById(Long applicationId, String scope, Long userId) {
        return applicationMapper.selectApplicationById(applicationId, scope, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int submitApplication(GwMobileApplication application) {
        if (application.getReason() == null || application.getReason().isBlank()) throw new ServiceException("申请原因不能为空");
        if (application.getPlannedStart() == null || application.getPlannedEnd() == null) throw new ServiceException("拟使用开始和结束日期不能为空");
        if (application.getPlannedEnd().before(application.getPlannedStart())) throw new ServiceException("结束日期不能早于开始日期");
        if (profileMapper.selectMentorId(application.getStudentId()) == null) {
            throw new ServiceException("尚未绑定导师，请联系管理员完成师生关系绑定后再提交");
        }
        if (applicationMapper.countActiveAssignmentByStudent(application.getStudentId()) > 0) {
            throw new ServiceException("您已有有效工位，不能重复申请流动工位");
        }
        Long mentorId = profileMapper.selectMentorId(application.getStudentId());
        if (workstationMapper.countAvailableFixedByMentor(mentorId) > 0) {
            throw new ServiceException("导师名下仍有空闲固定工位，请先由导师分配固定工位");
        }
        if (applicationMapper.countPendingByStudent(application.getStudentId()) > 0) {
            throw new ServiceException("存在未办结的流动工位申请，不能重复提交");
        }
        int rows = applicationMapper.insertApplication(application);
        lifecycle.workflowChanged(application.getApplicationId(), false);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int mentorReview(Long applicationId, Long mentorId, String status, String comment) {
        if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) {
            throw new ServiceException("导师审核状态不合法");
        }
        if (comment == null || comment.isBlank()) throw new ServiceException("请填写导师审核意见");
        GwMobileApplication application = applicationMapper.selectApplicationForUpdate(applicationId);
        if (application == null || !"PENDING_MENTOR".equals(application.getStatus())
            || !mentorId.equals(profileMapper.selectMentorId(application.getStudentId()))) {
            throw new ServiceException("申请不存在、不在待导师审核状态或不属于您的学生");
        }
        if ("APPROVED".equals(status)) {
            if (applicationMapper.countActiveAssignmentByStudent(application.getStudentId()) > 0) throw new ServiceException("该学生已有有效工位");
            if (workstationMapper.countAvailableFixedByMentor(mentorId) > 0) throw new ServiceException("您名下仍有空闲固定工位，请先给学生分配固定工位，不能同意流动申请");
        }
        int rows = applicationMapper.updateMentorReview(applicationId, mentorId,
            "APPROVED".equals(status) ? "PENDING_ADMIN" : "REJECTED", comment);
        if (rows == 0) {
            throw new ServiceException("申请不存在、不在待导师审核状态或不属于您的学生");
        }
        lifecycle.workflowChanged(applicationId, false);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int adminReview(Long applicationId, String status, String comment, Long workstationId, Long operatorId) {
        if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) {
            throw new ServiceException("管理员审核状态不合法");
        }
        if (comment == null || comment.isBlank()) throw new ServiceException("请填写管理员审核意见");
        GwMobileApplication application = applicationMapper.selectApplicationForUpdate(applicationId);
        if (application == null || !"PENDING_ADMIN".equals(application.getStatus())) throw new ServiceException("申请不存在或不在待管理员审核状态");
        Long mentorId = profileMapper.selectMentorId(application.getStudentId());
        if ("APPROVED".equals(status)) {
            if (applicationMapper.countActiveAssignmentByStudent(application.getStudentId()) > 0) throw new ServiceException("该学生已有有效工位");
            if (mentorId != null && workstationMapper.countAvailableFixedByMentor(mentorId) > 0) throw new ServiceException("该导师名下出现空闲固定工位，应优先分配固定工位");
        }
        int rows = applicationMapper.updateAdminReview(applicationId,
            "APPROVED".equals(status) ? "WAIT_ALLOC" : "REJECTED", comment.trim());
        if (rows == 0) {
            throw new ServiceException("申请不存在或不在待管理员审核状态");
        }
        if ("APPROVED".equals(status) && workstationId != null) assignWorkstation(applicationId, workstationId, operatorId);
        lifecycle.workflowChanged(applicationId, false);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignWorkstation(Long applicationId, Long workstationId, Long operatorId) {
        GwMobileApplication application = applicationMapper.selectApplicationForUpdate(applicationId);
        if (application == null || !"WAIT_ALLOC".equals(application.getStatus())) {
            throw new ServiceException("申请不存在或未进入待分配状态");
        }
        GwWorkstation workstation = workstationMapper.selectWorkstationForUpdate(workstationId);
        if (workstation == null || !"AVAILABLE".equals(workstation.getStatus()) || !"MOBILE".equals(workstation.getWorkstationType())
            || !"NORMAL".equals(workstation.getRoomStatus())) {
            throw new ServiceException("目标工位不存在、所属房间不可用或不是流动工位");
        }
        if (applicationMapper.countActiveAssignmentByStudent(application.getStudentId()) > 0) {
            throw new ServiceException("该学生已有有效工位，不能重复分配");
        }
        Long mentorId = profileMapper.selectMentorId(application.getStudentId());
        if (mentorId != null && workstationMapper.countAvailableFixedByMentor(mentorId) > 0) {
            throw new ServiceException("该导师名下仍有空闲固定工位，不能分配流动工位");
        }
        try {
            applicationMapper.insertAssignment(application.getStudentId(), workstationId, applicationId, operatorId);
        } catch (DataIntegrityViolationException ex) {
            throw new ServiceException("工位或学生已被其他管理员分配，请刷新后重试");
        }
        if (workstationMapper.updateStatus(workstationId, "IN_USE") == 0
            || applicationMapper.updateApplicationStatus(applicationId, "COMPLETED", workstationId) == 0) {
            throw new ServiceException("分配状态更新失败，事务已回滚");
        }
        lifecycle.workflowChanged(applicationId, false);
    }
}
