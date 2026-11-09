package com.ruoyi.system.domain.gongwei;

import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.annotation.Excel;

/**
 * 工位台账及布局信息。
 */
public class GwWorkstation extends BaseEntity {
    private Long workstationId;
    @Excel(name = "房间ID", prompt = "请先在房间管理中查询ID；类型由房间自动确定")
    private Long roomId;
    private String roomName;
    private String roomType;
    private String roomStatus;
    private Long mentorId;
    private String mentorName;
    @Excel(name = "工位编号")
    private String workstationNo;
    @Excel(name = "工位类型", prompt = "FIXED=固定，MOBILE=流动")
    private String workstationType;
    @Excel(name = "布局行", prompt = "正整数")
    private Integer layoutRow;
    @Excel(name = "布局列", prompt = "正整数")
    private Integer layoutCol;
    @Excel(name = "初始状态", prompt = "AVAILABLE=空闲，DISABLED=停用")
    private String status;
    private Long studentId;
    private String studentName;

    public Long getWorkstationId() { return workstationId; }
    public void setWorkstationId(Long workstationId) { this.workstationId = workstationId; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public String getRoomStatus() { return roomStatus; }
    public void setRoomStatus(String roomStatus) { this.roomStatus = roomStatus; }
    public Long getMentorId() { return mentorId; }
    public void setMentorId(Long mentorId) { this.mentorId = mentorId; }
    public String getMentorName() { return mentorName; }
    public void setMentorName(String mentorName) { this.mentorName = mentorName; }
    public String getWorkstationNo() { return workstationNo; }
    public void setWorkstationNo(String workstationNo) { this.workstationNo = workstationNo; }
    public String getWorkstationType() { return workstationType; }
    public void setWorkstationType(String workstationType) { this.workstationType = workstationType; }
    public Integer getLayoutRow() { return layoutRow; }
    public void setLayoutRow(Integer layoutRow) { this.layoutRow = layoutRow; }
    public Integer getLayoutCol() { return layoutCol; }
    public void setLayoutCol(Integer layoutCol) { this.layoutCol = layoutCol; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
}
