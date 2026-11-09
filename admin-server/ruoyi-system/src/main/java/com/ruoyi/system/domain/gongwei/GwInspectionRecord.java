package com.ruoyi.system.domain.gongwei;

import java.util.Date;

/** 卫生检查记录。 */
public class GwInspectionRecord {
    private Long recordId;
    private Long taskId;
    private Long inspectorId;
    private String inspectorName;
    private Long roomId;
    private String roomName;
    private Long workstationId;
    private String workstationNo;
    private Integer score;
    private String grade;
    private String explanation;
    private String issueDescription;
    private String photoUrls;
    private String status;
    private String taskStatus;
    private String adminComment;
    private Date createTime;

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getInspectorId() { return inspectorId; }
    public void setInspectorId(Long inspectorId) { this.inspectorId = inspectorId; }
    public String getInspectorName() { return inspectorName; }
    public void setInspectorName(String inspectorName) { this.inspectorName = inspectorName; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public Long getWorkstationId() { return workstationId; }
    public void setWorkstationId(Long workstationId) { this.workstationId = workstationId; }
    public String getWorkstationNo() { return workstationNo; }
    public void setWorkstationNo(String workstationNo) { this.workstationNo = workstationNo; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public String getIssueDescription() { return issueDescription; }
    public void setIssueDescription(String issueDescription) { this.issueDescription = issueDescription; }
    public String getPhotoUrls() { return photoUrls; }
    public void setPhotoUrls(String photoUrls) { this.photoUrls = photoUrls; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTaskStatus() { return taskStatus; }
    public void setTaskStatus(String taskStatus) { this.taskStatus = taskStatus; }
    public String getAdminComment() { return adminComment; }
    public void setAdminComment(String adminComment) { this.adminComment = adminComment; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
