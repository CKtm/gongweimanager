package com.ruoyi.system.domain.gongwei;

import com.ruoyi.common.annotation.Excel;

/** 人员账号、业务身份与师生关系批量导入行。 */
public class GwPersonImportRow {
    @Excel(name = "登录账号", prompt = "通常使用学号/工号；跨类型可能重复时请增加 S_ / T_ 前缀")
    private String userName;
    @Excel(name = "姓名")
    private String nickName;
    @Excel(name = "学号/工号")
    private String identityNo;
    @Excel(name = "身份类型", prompt = "STUDENT / MENTOR / INSPECTOR")
    private String userType;
    @Excel(name = "导师工号", prompt = "学生可填写；导师须已存在或在同一文件内")
    private String mentorIdentityNo;
    @Excel(name = "部门ID", prompt = "可留空")
    private Long deptId;
    @Excel(name = "手机号码")
    private String phonenumber;
    @Excel(name = "邮箱")
    private String email;

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getNickName() { return nickName; }
    public void setNickName(String nickName) { this.nickName = nickName; }
    public String getIdentityNo() { return identityNo; }
    public void setIdentityNo(String identityNo) { this.identityNo = identityNo; }
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
    public String getMentorIdentityNo() { return mentorIdentityNo; }
    public void setMentorIdentityNo(String mentorIdentityNo) { this.mentorIdentityNo = mentorIdentityNo; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getPhonenumber() { return phonenumber; }
    public void setPhonenumber(String phonenumber) { this.phonenumber = phonenumber; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
