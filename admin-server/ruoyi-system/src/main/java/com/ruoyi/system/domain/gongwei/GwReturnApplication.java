package com.ruoyi.system.domain.gongwei;

import java.util.Date;

/** 工位退还与回收申请。 */
public class GwReturnApplication {
    private Long returnId;
    private Long assignmentId;
    private Long studentId;
    private String studentName;
    private Long workstationId;
    private String roomName;
    private String workstationNo;
    private String workstationType;
    private String sourceType;
    private String status;
    private String reason;
    private String mentorComment;
    private String adminComment;
    private String photoUrls;
    private Date photoUploadTime;
    private Boolean studentCleared;
    private Date createTime;

    public Long getReturnId() { return returnId; }
    public void setReturnId(Long returnId) { this.returnId = returnId; }
    public Long getAssignmentId() { return assignmentId; }
    public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public Long getWorkstationId() { return workstationId; }
    public void setWorkstationId(Long workstationId) { this.workstationId = workstationId; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public String getWorkstationNo() { return workstationNo; }
    public void setWorkstationNo(String workstationNo) { this.workstationNo = workstationNo; }
    public String getWorkstationType() { return workstationType; }
    public void setWorkstationType(String workstationType) { this.workstationType = workstationType; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getMentorComment() { return mentorComment; }
    public void setMentorComment(String mentorComment) { this.mentorComment = mentorComment; }
    public String getAdminComment() { return adminComment; }
    public void setAdminComment(String adminComment) { this.adminComment = adminComment; }
    public String getPhotoUrls() { return photoUrls; }
    public void setPhotoUrls(String photoUrls) { this.photoUrls = photoUrls; }
    public Date getPhotoUploadTime() { return photoUploadTime; }
    public void setPhotoUploadTime(Date photoUploadTime) { this.photoUploadTime = photoUploadTime; }
    public Boolean getStudentCleared() { return studentCleared; }
    public void setStudentCleared(Boolean studentCleared) { this.studentCleared = studentCleared; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
