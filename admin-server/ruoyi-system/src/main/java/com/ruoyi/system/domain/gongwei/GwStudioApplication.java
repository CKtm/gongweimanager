package com.ruoyi.system.domain.gongwei;

import java.util.Date;

/**
 * 导师工作室申请。工作室按整间独占，一个导师可以获批多间。
 */
public class GwStudioApplication {
    private Long studioApplicationId;
    private Long mentorId;
    private String mentorName;
    private Long roomId;
    private String roomName;
    private String location;
    private Integer workstationCount;
    private String reason;
    private String status;
    private String adminComment;
    private Long reviewerId;
    private String reviewerName;
    private Date reviewedTime;
    private Date createTime;

    public Long getStudioApplicationId() { return studioApplicationId; }
    public void setStudioApplicationId(Long studioApplicationId) { this.studioApplicationId = studioApplicationId; }
    public Long getMentorId() { return mentorId; }
    public void setMentorId(Long mentorId) { this.mentorId = mentorId; }
    public String getMentorName() { return mentorName; }
    public void setMentorName(String mentorName) { this.mentorName = mentorName; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Integer getWorkstationCount() { return workstationCount; }
    public void setWorkstationCount(Integer workstationCount) { this.workstationCount = workstationCount; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAdminComment() { return adminComment; }
    public void setAdminComment(String adminComment) { this.adminComment = adminComment; }
    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }
    public String getReviewerName() { return reviewerName; }
    public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }
    public Date getReviewedTime() { return reviewedTime; }
    public void setReviewedTime(Date reviewedTime) { this.reviewedTime = reviewedTime; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
