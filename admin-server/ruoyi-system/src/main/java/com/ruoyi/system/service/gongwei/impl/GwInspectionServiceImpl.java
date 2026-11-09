package com.ruoyi.system.service.gongwei.impl;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.gongwei.GwInspectionRecord;
import com.ruoyi.system.domain.gongwei.GwInspectionTask;
import com.ruoyi.system.domain.gongwei.GwRoom;
import com.ruoyi.system.domain.gongwei.GwWorkstation;
import com.ruoyi.system.mapper.gongwei.GwInspectionMapper;
import com.ruoyi.system.mapper.gongwei.GwRoomMapper;
import com.ruoyi.system.mapper.gongwei.GwUserProfileMapper;
import com.ruoyi.system.mapper.gongwei.GwWorkstationMapper;
import com.ruoyi.system.service.gongwei.IGwInspectionService;

@Service
public class GwInspectionServiceImpl implements IGwInspectionService {
    @Autowired private GwInspectionMapper mapper;
    @Autowired private GwUserProfileMapper profileMapper;
    @Autowired private GwRoomMapper roomMapper;
    @Autowired private GwWorkstationMapper workstationMapper;
    public List<GwInspectionTask> selectTaskList(Long inspectorId, String status) { return mapper.selectTaskList(inspectorId, status); }
    public int createTask(GwInspectionTask task) {
        if (task.getTaskName() == null || task.getTaskName().isBlank() || task.getInspectorId() == null || task.getScheduledDate() == null) throw new ServiceException("任务名称、检查员和日期不能为空");
        if (profileMapper.countActiveUserByRole(task.getInspectorId(), "inspector") == 0) throw new ServiceException("所选人员不是有效的卫生检查员");
        task.setRoomId(null);
        return mapper.insertTask(task);
    }
    @Transactional(rollbackFor = Exception.class)
    public int completeTask(Long taskId, Long inspectorId) {
        GwInspectionTask task = mapper.selectTaskForUpdate(taskId);
        if (task == null || !inspectorId.equals(task.getInspectorId())) throw new ServiceException("任务不存在或不属于当前检查员");
        if (!Arrays.asList("PENDING", "IN_PROGRESS").contains(task.getStatus())) throw new ServiceException("该任务当前不能完成");
        if (mapper.countRecords(taskId) == 0) throw new ServiceException("请至少提交一条评分再完成任务");
        int rows = mapper.updateTaskStatus(taskId, task.getStatus(), "SUBMITTED");
        if (rows == 0) throw new ServiceException("任务状态已变化，请刷新后重试");
        return rows;
    }
    public List<GwInspectionRecord> selectRecordList(Long inspectorId, String status) { return mapper.selectRecordList(inspectorId, status); }
    @Transactional(rollbackFor = Exception.class)
    public int submitRecord(GwInspectionRecord record) {
        if (record.getTaskId() == null || record.getScore() == null) throw new ServiceException("任务和卫生评分不能为空");
        if (record.getScore() < 0 || record.getScore() > 100) throw new ServiceException("卫生评分必须在0到100分之间");
        record.setGrade(gradeFromScore(record.getScore()));
        GwInspectionTask task = mapper.selectTaskForUpdate(record.getTaskId());
        if (task == null || !record.getInspectorId().equals(task.getInspectorId())) throw new ServiceException("任务不存在或不属于当前检查员");
        if (!Arrays.asList("PENDING", "IN_PROGRESS").contains(task.getStatus())) throw new ServiceException("该任务已完成，不能继续录入评分");
        if (record.getWorkstationId() != null) {
            GwWorkstation workstation = workstationMapper.selectWorkstationById(record.getWorkstationId());
            if (workstation == null || workstation.getStudentId() == null) throw new ServiceException("请选择学生当前使用的工位");
            record.setRoomId(workstation.getRoomId());
        } else {
            GwRoom room = record.getRoomId() == null ? null : roomMapper.selectRoomById(record.getRoomId());
            if (room == null || !"STUDIO".equals(room.getRoomType())) throw new ServiceException("请选择需要评分的工作室");
        }
        if (record.getIssueDescription() != null && !record.getIssueDescription().isBlank()
            && (record.getPhotoUrls() == null || record.getPhotoUrls().isBlank())) throw new ServiceException("记录问题时至少上传一张现场照片");
        if (mapper.countRecordTarget(record.getTaskId(), record.getRoomId(), record.getWorkstationId()) > 0) throw new ServiceException("该任务对象已经提交过检查记录");
        try {
            int rows = mapper.insertRecord(record);
            if (rows == 0) throw new ServiceException("检查记录提交失败");
            if ("PENDING".equals(task.getStatus()) && mapper.updateTaskStatus(task.getTaskId(), "PENDING", "IN_PROGRESS") == 0) {
                throw new ServiceException("任务状态已变化，请刷新后重试");
            }
            return rows;
        } catch (DataIntegrityViolationException ex) {
            throw new ServiceException("该任务对象已经提交过检查记录");
        }
    }
    @Transactional(rollbackFor = Exception.class)
    public int verifyRecord(Long recordId, String status, String comment, Long operatorId) {
        if (!Arrays.asList("VERIFIED", "ARCHIVED").contains(status)) throw new ServiceException("检查记录状态不合法");
        if (comment == null || comment.isBlank()) throw new ServiceException("请填写核实或归档意见");
        GwInspectionRecord record = mapper.selectRecordById(recordId);
        if (record == null) throw new ServiceException("检查记录不存在");
        GwInspectionTask task = mapper.selectTaskForUpdate(record.getTaskId());
        if (task == null || !"SUBMITTED".equals(task.getStatus())) throw new ServiceException("检查任务尚未提交或已经关闭，不能核实记录");
        String fromStatus = "VERIFIED".equals(status) ? "SUBMITTED" : "VERIFIED";
        int rows = mapper.updateRecordVerify(recordId, fromStatus, status, comment, operatorId);
        if (rows == 0) throw new ServiceException("检查记录状态已变化，请刷新后重试");
        if ("ARCHIVED".equals(status) && mapper.countNotArchivedRecords(record.getTaskId()) == 0
            && mapper.updateTaskStatus(record.getTaskId(), "SUBMITTED", "CLOSED") == 0) {
            throw new ServiceException("任务状态已变化，请刷新后重试");
        }
        return rows;
    }

    private String gradeFromScore(int score) {
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 60) return "C";
        return "D";
    }
}
