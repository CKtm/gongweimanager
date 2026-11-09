package com.ruoyi.common.core.domain.model;

/** 登录结果：正常登录签发 token，初始密码登录只签发一次性改密票据。 */
public class LoginResult
{
    private String token;
    private boolean passwordChangeRequired;
    private String resetToken;

    public static LoginResult success(String token)
    {
        LoginResult result = new LoginResult();
        result.setToken(token);
        return result;
    }

    public static LoginResult passwordChangeRequired(String resetToken)
    {
        LoginResult result = new LoginResult();
        result.setPasswordChangeRequired(true);
        result.setResetToken(resetToken);
        return result;
    }

    public String getToken()
    {
        return token;
    }

    public void setToken(String token)
    {
        this.token = token;
    }

    public boolean isPasswordChangeRequired()
    {
        return passwordChangeRequired;
    }

    public void setPasswordChangeRequired(boolean passwordChangeRequired)
    {
        this.passwordChangeRequired = passwordChangeRequired;
    }

    public String getResetToken()
    {
        return resetToken;
    }

    public void setResetToken(String resetToken)
    {
        this.resetToken = resetToken;
    }
}
