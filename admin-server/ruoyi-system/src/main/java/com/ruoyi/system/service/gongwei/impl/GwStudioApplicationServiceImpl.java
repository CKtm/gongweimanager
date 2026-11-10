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
    @Autowired private com.ruoyi.system.mapper.gongwei.GwWorkstationMapper workstationMapper;

    @Override
    public List<GwStudioApplication> selectList(String status, Long mentorId) {
        return studioMapper.selectList(status, mentorId);
    }

    @Override
    public int submit(Long mentorId, Long roomId, String reason) {
        throw new ServiceException("工作室申请已停用，固定工位由管理员直接划分给导师");
    }

    @Override
    public int review(Long studioApplicationId, String status, String comment, Long reviewerId) {
        throw new ServiceException("工作室申请审批已停用，请在工位管理中直接划分导师");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int release(Long roomId, String reason, Long operatorId) {
        if (reason == null || reason.isBlank()) throw new ServiceException("解除原因不能为空");
        GwRoom room = roomMapper.selectRoomForUpdate(roomId);
        if (room == null || !"STUDIO".equals(room.getRoomType())) throw new ServiceException("工作室不存在");
        List<com.ruoyi.system.domain.gongwei.GwWorkstation> seats = workstationMapper.selectRoomWorkstationsForUpdate(roomId);
        java.util.Set<Long> owners = new java.util.HashSet<>();
        for (var seat : seats) {
            if (seat.getMentorId() != null) owners.add(seat.getMentorId());
            if (seat.getStudentId() != null || !java.util.Set.of("AVAILABLE", "DISABLED").contains(seat.getStatus())) throw new ServiceException("工作室仍有使用中或回收中的工位，请先释放");
        }
        if (owners.size() != 1) throw new ServiceException("共享工作室请在工位管理中逐个或批量解除导师归属");
        Long mentorId = owners.iterator().next();
        for (var seat : seats) workstationMapper.updateMentor(seat.getWorkstationId(), null, "studio-release:" + operatorId);
        studioMapper.markReturned(roomId, mentorId, reason.trim(), operatorId);
        studioMapper.insertAudit(null, "RELEASE", operatorId, roomId, mentorId, reason.trim());
        return 1;
    }
}
