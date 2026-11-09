package com.ruoyi.system.domain.gongwei;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

/**
 * 流动工位申请。
 */
public class GwMobileApplication {
    private Long applicationId;
    private Long studentId;
    private String studentName;
    private String reason;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date plannedStart;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date plannedEnd;
    private String status;
    private String mentorComment;
    private String adminComment;
    private Long workstationId;
    private String workstationNo;
    private Date createTime;

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Date getPlannedStart() { return plannedStart; }
    public void setPlannedStart(Date plannedStart) { this.plannedStart = plannedStart; }
    public Date getPlannedEnd() { return plannedEnd; }
    public void setPlannedEnd(Date plannedEnd) { this.plannedEnd = plannedEnd; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMentorComment() { return mentorComment; }
    public void setMentorComment(String mentorComment) { this.mentorComment = mentorComment; }
    public String getAdminComment() { return adminComment; }
    public void setAdminComment(String adminComment) { this.adminComment = adminComment; }
    public Long getWorkstationId() { return workstationId; }
    public void setWorkstationId(Long workstationId) { this.workstationId = workstationId; }
    public String getWorkstationNo() { return workstationNo; }
    public void setWorkstationNo(String workstationNo) { this.workstationNo = workstationNo; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
