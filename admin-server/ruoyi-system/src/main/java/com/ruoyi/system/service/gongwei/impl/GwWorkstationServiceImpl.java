package com.ruoyi.system.service.gongwei.impl;

import java.util.List;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.gongwei.GwWorkstation;
import com.ruoyi.system.mapper.gongwei.GwWorkstationMapper;
import com.ruoyi.system.mapper.gongwei.GwRoomMapper;
import com.ruoyi.system.domain.gongwei.GwRoom;
import com.ruoyi.system.service.gongwei.IGwWorkstationService;

@Service
public class GwWorkstationServiceImpl implements IGwWorkstationService {
    @Autowired
    private GwWorkstationMapper workstationMapper;
    @Autowired
    private GwRoomMapper roomMapper;
    @Autowired
    private com.ruoyi.system.mapper.gongwei.GwUserProfileMapper profileMapper;

    @Override
    public List<GwWorkstation> selectWorkstationList(GwWorkstation workstation) {
        return workstationMapper.selectWorkstationList(workstation);
    }
    @Override
    public GwWorkstation selectWorkstationById(Long workstationId) {
        return workstationMapper.selectWorkstationById(workstationId);
    }
    @Override
    public int insertWorkstation(GwWorkstation workstation) {
        applyRoomType(workstation, false);
        validate(workstation);
        if (!"AVAILABLE".equals(workstation.getStatus()) && !"DISABLED".equals(workstation.getStatus())) throw new ServiceException("新工位只能设为空闲或停用");
        return workstationMapper.insertWorkstation(workstation);
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateWorkstation(GwWorkstation workstation) {
        applyRoomType(workstation, false);
        validate(workstation);
        GwWorkstation current = workstationMapper.selectWorkstationForUpdate(workstation.getWorkstationId());
        if (current == null) throw new ServiceException("工位不存在");
        if (!current.getRoomId().equals(workstation.getRoomId()) && workstationMapper.countWorkstationReferences(workstation.getWorkstationId()) > 0) {
            throw new ServiceException("工位已有业务历史，不能迁移到其他房间；请在目标房间新建工位");
        }
        if (("IN_USE".equals(workstation.getStatus()) || "PENDING_RETURN".equals(workstation.getStatus()))
            && !workstation.getStatus().equals(current.getStatus())) {
            throw new ServiceException("占用和待回收状态只能由分配、退回流程变更");
        }
        if (current.getStudentId() != null || "IN_USE".equals(current.getStatus()) || "PENDING_RETURN".equals(current.getStatus())) {
            if (!current.getRoomId().equals(workstation.getRoomId())
                || !current.getWorkstationType().equals(workstation.getWorkstationType())
                || !current.getStatus().equals(workstation.getStatus())
                || !java.util.Objects.equals(current.getMentorId(), workstation.getMentorId())) {
                throw new ServiceException("使用中或待回收工位不能改变所属房间、归属导师、类型或状态");
            }
        }
        return workstationMapper.updateWorkstation(workstation);
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteWorkstationById(Long workstationId) {
        GwWorkstation current = workstationMapper.selectWorkstationForUpdate(workstationId);
        if (current == null) throw new ServiceException("工位不存在");
        if (!"AVAILABLE".equals(current.getStatus()) || current.getStudentId() != null) throw new ServiceException("只有无占用的空闲工位可以删除");
        if (workstationMapper.countWorkstationReferences(workstationId) > 0) throw new ServiceException("工位存在分配历史、未结流程或检查记录，请改为停用");
        return workstationMapper.deleteWorkstationById(workstationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String importWorkstations(List<GwWorkstation> workstations, String operator) {
        if (workstations == null || workstations.isEmpty()) throw new ServiceException("导入数据不能为空");
        Set<String> fileKeys = new HashSet<>();
        int rowNo = 1;
        for (GwWorkstation workstation : workstations) {
            rowNo++;
            if (workstation == null) throw new ServiceException("第" + rowNo + "行数据为空");
            if (workstation.getWorkstationNo() != null) workstation.setWorkstationNo(workstation.getWorkstationNo().trim());
            if (workstation.getWorkstationType() != null) workstation.setWorkstationType(workstation.getWorkstationType().trim().toUpperCase(Locale.ROOT));
            workstation.setStatus(workstation.getStatus() == null || workstation.getStatus().isBlank() ? "AVAILABLE" : workstation.getStatus().trim().toUpperCase(Locale.ROOT));
            applyRoomType(workstation, true);
            validate(workstation);
            if (!"AVAILABLE".equals(workstation.getStatus()) && !"DISABLED".equals(workstation.getStatus())) throw new ServiceException("第" + rowNo + "行初始状态只能是 AVAILABLE 或 DISABLED");
            String key = workstation.getRoomId() + "#" + workstation.getWorkstationNo().toLowerCase(Locale.ROOT);
            if (!fileKeys.add(key)) throw new ServiceException("第" + rowNo + "行与导入文件内其他工位编号重复");
            if (workstationMapper.countByRoomAndNo(workstation.getRoomId(), workstation.getWorkstationNo()) > 0) throw new ServiceException("第" + rowNo + "行工位编号在该房间已存在");
            workstation.setCreateBy(operator);
        }
        for (GwWorkstation workstation : workstations) workstationMapper.insertWorkstation(workstation);
        return "成功导入 " + workstations.size() + " 个工位";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int assignMentor(List<Long> workstationIds, Long mentorId, String operator) {
        if (workstationIds == null || workstationIds.isEmpty() || workstationIds.size() > 200 || workstationIds.stream().anyMatch(java.util.Objects::isNull)) throw new ServiceException("请选择1至200个固定工位");
        validateMentor(mentorId);
        List<Long> ids = workstationIds.stream().distinct().sorted().toList();
        for (Long id : ids) {
            GwWorkstation seat = workstationMapper.selectWorkstationForUpdate(id);
            if (seat == null || !"FIXED".equals(seat.getWorkstationType()) || !"STUDIO".equals(seat.getRoomType())) throw new ServiceException("只能划分工作室内的固定工位");
            if (!java.util.Objects.equals(seat.getMentorId(), mentorId) && (seat.getStudentId() != null || !java.util.Set.of("AVAILABLE", "DISABLED").contains(seat.getStatus()))) throw new ServiceException("工位使用中或正在回收，请先释放后再更换导师");
            workstationMapper.updateMentor(id, mentorId, operator);
        }
        return ids.size();
    }

    private void validateMentor(Long mentorId) {
        if (mentorId != null && profileMapper.countActiveUserByRole(mentorId, "mentor") == 0) throw new ServiceException("归属导师不存在、已停用或不具备导师角色");
    }

    private void validate(GwWorkstation workstation) {
        if (workstation == null || workstation.getRoomId() == null || workstation.getWorkstationNo() == null || workstation.getWorkstationNo().isBlank()) throw new ServiceException("房间和工位编号不能为空");
        if (!"FIXED".equals(workstation.getWorkstationType()) && !"MOBILE".equals(workstation.getWorkstationType())) throw new ServiceException("工位类型不合法");
        if (workstation.getStatus() == null) workstation.setStatus("AVAILABLE");
        if (!"AVAILABLE".equals(workstation.getStatus()) && !"DISABLED".equals(workstation.getStatus()) && !"IN_USE".equals(workstation.getStatus()) && !"PENDING_RETURN".equals(workstation.getStatus())) throw new ServiceException("工位状态不合法");
        if ((workstation.getLayoutRow() != null && workstation.getLayoutRow() < 0) || (workstation.getLayoutCol() != null && workstation.getLayoutCol() < 0)) throw new ServiceException("布局行列不能为负数");
    }

    private void applyRoomType(GwWorkstation workstation, boolean importing) {
        if (workstation == null || workstation.getRoomId() == null) throw new ServiceException("房间不能为空");
        GwRoom room = roomMapper.selectRoomById(workstation.getRoomId());
        if (room == null || !"NORMAL".equals(room.getStatus())) throw new ServiceException((importing ? "导入数据中的" : "目标") + "房间不存在或不可用");
        String expected = "STUDIO".equals(room.getRoomType()) ? "FIXED" : "MOBILE";
        if (workstation.getWorkstationType() != null && !workstation.getWorkstationType().isBlank()
            && !expected.equals(workstation.getWorkstationType())) {
            throw new ServiceException("工位类型必须与房间类型一致：工作室只能创建固定工位，流动房间只能创建流动工位");
        }
        workstation.setWorkstationType(expected);
        if ("MOBILE".equals(expected) && workstation.getMentorId() != null) throw new ServiceException("流动工位不能划归导师");
        validateMentor(workstation.getMentorId());
    }
}
