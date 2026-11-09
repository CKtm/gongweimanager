package com.ruoyi.common.core.domain.model;

/** 首次登录强制修改密码请求。 */
public class ForcePasswordBody
{
    private String resetToken;
    private String newPassword;

    public String getResetToken()
    {
        return resetToken;
    }

    public void setResetToken(String resetToken)
    {
        this.resetToken = resetToken;
    }

    public String getNewPassword()
    {
        return newPassword;
    }

    public void setNewPassword(String newPassword)
    {
        this.newPassword = newPassword;
    }
}
