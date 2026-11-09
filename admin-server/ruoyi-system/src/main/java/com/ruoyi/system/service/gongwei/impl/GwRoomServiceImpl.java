package com.ruoyi.system.service.gongwei.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.gongwei.GwRoom;
import com.ruoyi.system.mapper.gongwei.GwRoomMapper;
import com.ruoyi.system.service.gongwei.IGwRoomService;

@Service
public class GwRoomServiceImpl implements IGwRoomService {
    @Autowired
    private GwRoomMapper roomMapper;

    @Override
    public List<GwRoom> selectRoomList(GwRoom room) { return roomMapper.selectRoomList(room); }
    @Override
    public GwRoom selectRoomById(Long roomId) { return roomMapper.selectRoomById(roomId); }
    @Override
    public int insertRoom(GwRoom room) {
        validate(room);
        return roomMapper.insertRoom(room);
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateRoom(GwRoom room) {
        validate(room);
        GwRoom current = roomMapper.selectRoomForUpdate(room.getRoomId());
        if (current == null) throw new ServiceException("房间不存在");
        boolean typeChanged = !current.getRoomType().equals(room.getRoomType());
        if ((typeChanged || !"NORMAL".equals(room.getStatus())) && roomMapper.countPendingStudioApplications(room.getRoomId()) > 0) {
            throw new ServiceException("房间存在待审核的工作室申请，请先完成审批或驳回");
        }
        if (typeChanged && (roomMapper.countWorkstations(room.getRoomId()) > 0 || current.getMentorId() != null)) {
            throw new ServiceException("房间已有工位或导师归属，不能变更房间类型");
        }
        if (!"NORMAL".equals(room.getStatus()) && roomMapper.countActiveAssignments(room.getRoomId()) > 0) {
            throw new ServiceException("房间内仍有学生使用工位，不能停用或设置维修");
        }
        return roomMapper.updateRoom(room);
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteRoomById(Long roomId) {
        GwRoom room = roomMapper.selectRoomForUpdate(roomId);
        if (room == null) throw new ServiceException("房间不存在");
        if (room.getMentorId() != null) throw new ServiceException("工作室仍绑定导师，请先解除归属");
        if (roomMapper.countRoomReferences(roomId) > 0) throw new ServiceException("房间存在工位、申请历史或卫生检查任务，请改为停用");
        return roomMapper.deleteRoomById(roomId);
    }

    private void validate(GwRoom room) {
        if (room == null || room.getRoomName() == null || room.getRoomName().isBlank()) throw new ServiceException("房间名称不能为空");
        if (!"STUDIO".equals(room.getRoomType()) && !"MOBILE".equals(room.getRoomType())) throw new ServiceException("房间类型必须是工作室或流动工位房间");
        if (room.getStatus() == null || room.getStatus().isBlank()) room.setStatus("NORMAL");
        if (!"NORMAL".equals(room.getStatus()) && !"MAINTENANCE".equals(room.getStatus()) && !"DISABLED".equals(room.getStatus())) throw new ServiceException("房间状态不合法");
    }
}
