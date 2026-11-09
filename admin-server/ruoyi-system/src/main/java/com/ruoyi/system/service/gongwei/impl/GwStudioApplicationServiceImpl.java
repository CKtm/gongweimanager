package com.ruoyi.system.service.gongwei.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.gongwei.GwRoom;
import com.ruoyi.system.domain.gongwei.GwStudioApplication;
import com.ruoyi.system.mapper.gongwei.GwRoomMapper;
import com.ruoyi.system.mapper.gongwei.GwStudioApplicationMapper;
import com.ruoyi.system.service.gongwei.IGwStudioApplicationService;

@Service
public class GwStudioApplicationServiceImpl implements IGwStudioApplicationService {
    @Autowired private GwStudioApplicationMapper studioMapper;
    @Autowired private GwRoomMapper roomMapper;

    @Override
    public List<GwStudioApplication> selectList(String status, Long mentorId) {
        return studioMapper.selectList(status, mentorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int submit(Long mentorId, Long roomId, String reason) {
        if (mentorId == null || roomId == null) throw new ServiceException("导师和工作室不能为空");
        if (reason == null || reason.isBlank()) throw new ServiceException("申请理由不能为空");
        GwRoom room = roomMapper.selectRoomForUpdate(roomId);
        if (room == null || !"STUDIO".equals(room.getRoomType())) throw new ServiceException("所选房间不是工作室");
        if (!"NORMAL".equals(room.getStatus())) throw new ServiceException("工作室当前不可申请");
        if (room.getMentorId() != null) throw new ServiceException("该工作室已归属其他导师");
        if (studioMapper.countPending(mentorId, roomId) > 0) throw new ServiceException("该工作室已有待审核申请，不能重复申请");
        GwStudioApplication application = new GwStudioApplication();
        application.setMentorId(mentorId); application.setRoomId(roomId); application.setReason(reason.trim());
        int rows = studioMapper.insertApplication(application);
        studioMapper.insertAudit(application.getStudioApplicationId(), "SUBMIT", mentorId, roomId, mentorId, reason.trim());
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int review(Long studioApplicationId, String status, String comment, Long reviewerId) {
        if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) throw new ServiceException("审批状态不合法");
        if (comment == null || comment.isBlank()) throw new ServiceException("审批意见不能为空");
        GwStudioApplication application = studioMapper.selectForUpdate(studioApplicationId);
        if (application == null || !"PENDING".equals(application.getStatus())) throw new ServiceException("申请不存在或已处理");
        if ("APPROVED".equals(status)) {
            GwRoom room = roomMapper.selectRoomForUpdate(application.getRoomId());
            if (room == null || !"STUDIO".equals(room.getRoomType()) || !"NORMAL".equals(room.getStatus())) throw new ServiceException("工作室不存在或不可用");
            if (room.getMentorId() != null) throw new ServiceException("该工作室已分配给其他导师，请驳回本申请");
            if (roomMapper.bindMentor(room.getRoomId(), application.getMentorId()) == 0) throw new ServiceException("工作室已被其他审批占用，请刷新后重试");
        }
        int rows = studioMapper.review(studioApplicationId, status, comment.trim(), reviewerId);
        if (rows == 0) throw new ServiceException("申请状态已变化，请刷新后重试");
        studioMapper.insertAudit(studioApplicationId, status, reviewerId, application.getRoomId(), application.getMentorId(), comment.trim());
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int release(Long roomId, String reason, Long operatorId) {
        if (reason == null || reason.isBlank()) throw new ServiceException("解除原因不能为空");
        GwRoom room = roomMapper.selectRoomForUpdate(roomId);
        if (room == null || !"STUDIO".equals(room.getRoomType()) || room.getMentorId() == null) throw new ServiceException("工作室未绑定导师");
        if (roomMapper.countActiveAssignments(roomId) > 0) throw new ServiceException("工作室仍有学生使用固定工位，请先完成退还或迁移");
        Long mentorId = room.getMentorId();
        if (roomMapper.releaseMentor(roomId, mentorId) == 0) throw new ServiceException("工作室归属已变化，请刷新后重试");
        studioMapper.markReturned(roomId, mentorId, reason.trim(), operatorId);
        studioMapper.insertAudit(null, "RELEASE", operatorId, roomId, mentorId, reason.trim());
        return 1;
    }
}
