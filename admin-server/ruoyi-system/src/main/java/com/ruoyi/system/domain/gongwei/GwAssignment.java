package com.ruoyi.system.domain.gongwei;

import java.util.Date;

/** 工位占用记录。 */
public class GwAssignment {
    private Long assignmentId;
    private Long studentId;
    private String studentName;
    private Long workstationId;
    private Long roomId;
    private String roomName;
    private String workstationNo;
    private String workstationType;
    private Long applicationId;
    private String termCode;
    private Date startedAt;
    private Date endedAt;
    private Long operatorId;

    public Long getAssignmentId() { return assignmentId; }
    public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public Long getWorkstationId() { return workstationId; }
    public void setWorkstationId(Long workstationId) { this.workstationId = workstationId; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public String getWorkstationNo() { return workstationNo; }
    public void setWorkstationNo(String workstationNo) { this.workstationNo = workstationNo; }
    public String getWorkstationType() { return workstationType; }
    public void setWorkstationType(String workstationType) { this.workstationType = workstationType; }
    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
    public String getTermCode() { return termCode; }
    public void setTermCode(String termCode) { this.termCode = termCode; }
    public Date getStartedAt() { return startedAt; }
    public void setStartedAt(Date startedAt) { this.startedAt = startedAt; }
    public Date getEndedAt() { return endedAt; }
    public void setEndedAt(Date endedAt) { this.endedAt = endedAt; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
}
