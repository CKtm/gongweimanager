package com.ruoyi.system.domain.gongwei;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

/** 卫生检查任务。 */
public class GwInspectionTask {
    private Long taskId;
    private String taskName;
    private Long inspectorId;
    private String inspectorName;
    private Long roomId;
    private String roomName;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date scheduledDate;
    private String status;
    private String remark;
    private String createBy;
    private Date createTime;
    private Integer recordCount;
    private Integer workstationRecordCount;
    private Integer roomScore;

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public Long getInspectorId() { return inspectorId; }
    public void setInspectorId(Long inspectorId) { this.inspectorId = inspectorId; }
    public String getInspectorName() { return inspectorName; }
    public void setInspectorName(String inspectorName) { this.inspectorName = inspectorName; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public Date getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(Date scheduledDate) { this.scheduledDate = scheduledDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Integer getRecordCount() { return recordCount; }
    public void setRecordCount(Integer recordCount) { this.recordCount = recordCount; }
    public Integer getWorkstationRecordCount() { return workstationRecordCount; }
    public void setWorkstationRecordCount(Integer workstationRecordCount) { this.workstationRecordCount = workstationRecordCount; }
    public Integer getRoomScore() { return roomScore; }
    public void setRoomScore(Integer roomScore) { this.roomScore = roomScore; }
}
