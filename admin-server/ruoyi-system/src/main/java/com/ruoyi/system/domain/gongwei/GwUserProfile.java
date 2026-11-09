package com.ruoyi.system.domain.gongwei;

/** 师生与检查员业务档案。 */
public class GwUserProfile {
    private Long userId;
    private String userName;
    private String nickName;
    private String identityNo;
    private String userType;
    private Long mentorId;
    private String mentorName;
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getNickName() { return nickName; }
    public void setNickName(String nickName) { this.nickName = nickName; }
    public String getIdentityNo() { return identityNo; }
    public void setIdentityNo(String identityNo) { this.identityNo = identityNo; }
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
    public Long getMentorId() { return mentorId; }
    public void setMentorId(Long mentorId) { this.mentorId = mentorId; }
    public String getMentorName() { return mentorName; }
    public void setMentorName(String mentorName) { this.mentorName = mentorName; }
}
