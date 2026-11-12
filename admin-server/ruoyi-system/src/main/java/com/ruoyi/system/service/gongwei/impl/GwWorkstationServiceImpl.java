package com.ruoyi.system.service.gongwei.impl;

import java.util.List;
import java.util.ArrayList;
import java.math.BigInteger;
import org.springframework.dao.DataIntegrityViolationException;
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
    @Autowired private com.ruoyi.system.service.gongwei.GwStudentOpsService lifecycle;
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
    @Transactional(rollbackFor = Exception.class)
    public int insertWorkstation(GwWorkstation workstation) {
        lockRoom(workstation == null ? null : workstation.getRoomId());
        applyRoomType(workstation, false);
        validate(workstation);
        if (!"AVAILABLE".equals(workstation.getStatus())) throw new ServiceException("新工位只能设为空闲；停用仅针对已分配学生的工位");
        return workstationMapper.insertWorkstation(workstation);
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateWorkstation(GwWorkstation workstation) {
        lockRoom(workstation == null ? null : workstation.getRoomId());
        applyRoomType(workstation, false);
        validate(workstation);
        GwWorkstation current = workstationMapper.selectWorkstationForUpdate(workstation.getWorkstationId());
        if (current == null) throw new ServiceException("工位不存在");
        if (!java.util.Objects.equals(current.getStatus(), workstation.getStatus())) throw new ServiceException("不能通过编辑修改工位状态，请使用停用、恢复或收回操作");
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
        if (workstationMapper.countPendingWorkstationFlows(workstationId) > 0) throw new ServiceException("工位存在未完成申请或退还流程，不能删除");
        return workstationMapper.deleteWorkstationById(workstationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String importWorkstations(List<GwWorkstation> workstations, String operator) {
        if (workstations == null || workstations.isEmpty()) throw new ServiceException("导入数据不能为空");
        // Use the same parent lock as automatic creation, in a stable order for multi-room imports.
        for (Long roomId : workstations.stream().filter(java.util.Objects::nonNull).map(GwWorkstation::getRoomId).filter(java.util.Objects::nonNull).distinct().sorted().toList()) lockRoom(roomId);
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
            if (!"AVAILABLE".equals(workstation.getStatus())) throw new ServiceException("第" + rowNo + "行初始状态只能是 AVAILABLE；停用仅针对已分配学生的工位");
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int createWorkstations(Long roomId, Integer count, String operator) {
        if (count == null || count < 1 || count > 200) throw new ServiceException("新增数量必须是1至200的整数");
        GwRoom room = lockRoom(roomId);
        if (!"STUDIO".equals(room.getRoomType()) && !"MOBILE".equals(room.getRoomType())) throw new ServiceException("房间类型不合法");
        List<GwWorkstation> existing = workstationMapper.selectRoomWorkstationsForUpdate(roomId);
        BigInteger largest = BigInteger.ZERO;
        int columns = 3;
        for (GwWorkstation seat : existing) {
            String number = seat.getWorkstationNo() == null ? "" : seat.getWorkstationNo().trim();
            if (number.matches("[0-9]+")) largest = largest.max(new BigInteger(number));
            if (seat.getLayoutCol() != null && seat.getLayoutCol() > columns) columns = seat.getLayoutCol();
        }
        long position = existing.size();
        for (GwWorkstation seat : existing) {
            if (seat.getLayoutRow() != null && seat.getLayoutRow() > 0 && seat.getLayoutCol() != null && seat.getLayoutCol() > 0) {
                position = Math.max(position, ((long) seat.getLayoutRow() - 1) * columns + seat.getLayoutCol());
            }
        }
        BigInteger nextNumber = largest.add(BigInteger.ONE);
        List<GwWorkstation> planned = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            while (true) {
                if (nextNumber.toString().length() > 50) throw new ServiceException("自动编号超过长度限制，请先检查已有工位编号");
                if (workstationMapper.countByRoomAndNo(roomId, nextNumber.toString()) == 0) break;
                nextNumber = nextNumber.add(BigInteger.ONE);
            }
            long row = position / columns + 1;
            if (row > Integer.MAX_VALUE) throw new ServiceException("自动布局超出范围，请先检查房间已有布局");
            GwWorkstation seat = new GwWorkstation();
            seat.setRoomId(roomId);
            seat.setWorkstationNo(nextNumber.toString());
            seat.setWorkstationType("STUDIO".equals(room.getRoomType()) ? "FIXED" : "MOBILE");
            seat.setMentorId(null);
            seat.setStatus("AVAILABLE");
            seat.setLayoutRow((int) row);
            seat.setLayoutCol((int) (position % columns) + 1);
            seat.setCreateBy(operator);
            planned.add(seat);
            nextNumber = nextNumber.add(BigInteger.ONE);
            position++;
        }
        try {
            for (GwWorkstation seat : planned) {
                if (workstationMapper.insertWorkstation(seat) != 1) throw new ServiceException("创建工位失败，本次新增已撤销");
            }
        } catch (DataIntegrityViolationException ex) {
            throw new ServiceException("工位编号发生冲突，本次新增已撤销，请刷新后重试");
        }
        return planned.size();
    }

    private List<Long> batchIds(List<Long> ids) {
        if (ids == null || ids.isEmpty() || ids.size() > 200 || ids.stream().anyMatch(java.util.Objects::isNull)) throw new ServiceException("请选择1至200个工位");
        return ids.stream().distinct().sorted().toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteWorkstations(List<Long> workstationIds) {
        List<Long> ids = batchIds(workstationIds);
        for (Long id : ids) {
            if (deleteWorkstationById(id) != 1) throw new ServiceException("删除工位失败，本次操作已撤销");
        }
        return ids.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int changeWorkstationStatus(List<Long> workstationIds, String status, String operator) {
        return changeWorkstationStatus(workstationIds, status, operator, null, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int changeWorkstationStatus(List<Long> workstationIds, String status, String operator, java.util.Date until, String reason, Long operatorId) {
        if ("DISABLED".equals(status)) lifecycle.validateDeadline(until, reason);
        if (!"IN_USE".equals(status) && !"DISABLED".equals(status)) throw new ServiceException("仅支持使用中工位停用、停用工位恢复使用");
        List<Long> ids = batchIds(workstationIds);
        List<GwWorkstation> seats = new ArrayList<>();
        for (Long id : ids) {
            GwWorkstation seat = workstationMapper.selectWorkstationForUpdate(id);
            if (seat == null) throw new ServiceException("工位 ID " + id + " 不存在，本次操作已撤销");
            String label = seat.getRoomName() + " / " + seat.getWorkstationNo();
            String expected = "DISABLED".equals(status) ? "IN_USE" : "DISABLED";
            if (seat.getStudentId() == null || !expected.equals(seat.getStatus())) throw new ServiceException("工位 " + label + ("DISABLED".equals(status) ? " 必须为已绑定学生的使用中工位才能停用" : " 必须为已绑定学生的停用工位才能恢复") + "，本次操作已撤销");
            if ("IN_USE".equals(status) && !"NORMAL".equals(seat.getRoomStatus())) throw new ServiceException("工位 " + label + " 所在房间不可用，不能恢复，本次操作已撤销");
            seats.add(seat);
        }
        for (GwWorkstation seat : seats) {
            if ("DISABLED".equals(status)) lifecycle.startPenalty(seat, until, reason, operatorId);
            else lifecycle.finishPenalty(seat.getWorkstationId(), "RESTORED", "管理员提前恢复工位使用权");
            seat.setStatus(status);
            seat.setUpdateBy(operator);
            if (workstationMapper.updateWorkstation(seat) != 1) throw new ServiceException("更新工位失败，本次操作已撤销");
        }
        return seats.size();
    }

    private GwRoom lockRoom(Long roomId) {
        if (roomId == null) throw new ServiceException("请选择房间");
        GwRoom room = roomMapper.selectRoomForUpdate(roomId);
        if (room == null || !"NORMAL".equals(room.getStatus())) throw new ServiceException("房间不存在、已停用或正在维修");
        return room;
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
