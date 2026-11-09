package com.ruoyi.system.domain.gongwei;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 工位工作室。
 */
public class GwRoom extends BaseEntity {
    private Long roomId;
    private String roomName;
    private String location;
    /** STUDIO=导师独占工作室，MOBILE=流动工位房间。 */
    private String roomType;
    /** 当前独占导师；一个导师可拥有多间工作室。 */
    private Long mentorId;
    private String mentorName;
    private String status;
    private String remark;
    private Integer workstationCount;
    private Integer availableCount;
    private Integer inUseCount;
    private Integer disabledCount;
    /** 查询条件：仅返回尚未绑定导师的可申请工作室。 */
    private Boolean availableForApply;

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public Long getMentorId() { return mentorId; }
    public void setMentorId(Long mentorId) { this.mentorId = mentorId; }
    public String getMentorName() { return mentorName; }
    public void setMentorName(String mentorName) { this.mentorName = mentorName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Integer getWorkstationCount() { return workstationCount; }
    public void setWorkstationCount(Integer workstationCount) { this.workstationCount = workstationCount; }
    public Integer getAvailableCount() { return availableCount; }
    public void setAvailableCount(Integer availableCount) { this.availableCount = availableCount; }
    public Integer getInUseCount() { return inUseCount; }
    public void setInUseCount(Integer inUseCount) { this.inUseCount = inUseCount; }
    public Integer getDisabledCount() { return disabledCount; }
    public void setDisabledCount(Integer disabledCount) { this.disabledCount = disabledCount; }
    public Boolean getAvailableForApply() { return availableForApply; }
    public void setAvailableForApply(Boolean availableForApply) { this.availableForApply = availableForApply; }
}
