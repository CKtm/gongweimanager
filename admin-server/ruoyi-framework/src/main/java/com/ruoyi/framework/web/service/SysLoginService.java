package com.ruoyi.framework.web.service;

import java.util.concurrent.TimeUnit;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginResult;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.exception.user.BlackListException;
import com.ruoyi.common.exception.user.CaptchaException;
import com.ruoyi.common.exception.user.CaptchaExpireException;
import com.ruoyi.common.exception.user.UserNotExistsException;
import com.ruoyi.common.exception.user.UserPasswordNotMatchException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.MessageUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.core.text.Convert;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.framework.manager.AsyncManager;
import com.ruoyi.framework.manager.factory.AsyncFactory;
import com.ruoyi.framework.security.context.AuthenticationContextHolder;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysUserService;

/**
 * 登录校验方法
 * 
 * @author ruoyi
 */
@Component
public class SysLoginService
{
    private static final int RESET_TOKEN_MINUTES = 5;
    private static final int NEW_PASSWORD_MIN_LENGTH = 6;

    @Autowired
    private TokenService tokenService;

    @Resource
    private AuthenticationManager authenticationManager;

    @Autowired
    private RedisCache redisCache;
    
    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysConfigService configService;

    /**
     * 登录验证
     * 
     * @param username 用户名
     * @param password 密码
     * @param code 验证码
     * @param uuid 唯一标识
     * @return 结果
     */
    public LoginResult login(String username, String password, String code, String uuid)
    {
        // 验证码校验
        validateCaptcha(username, code, uuid);
        // 登录前置校验
        loginPreCheck(username, password);
        LoginUser loginUser = authenticate(username, password);
        if (isInitialPasswordChangeRequired(loginUser.getUser()))
        {
            tokenService.deleteTokensByUserId(loginUser.getUserId());
            String resetToken = IdUtils.fastUUID();
            redisCache.setCacheObject(CacheConstants.PWD_RESET_TOKEN_KEY + resetToken,
                    String.valueOf(loginUser.getUserId()), RESET_TOKEN_MINUTES, TimeUnit.MINUTES);
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL,
                    "首次登录必须修改初始密码"));
            return LoginResult.passwordChangeRequired(resetToken);
        }
        AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_SUCCESS,
                MessageUtils.message("user.login.success")));
        recordLoginInfo(loginUser.getUserId());
        return LoginResult.success(tokenService.createToken(loginUser));
    }

    /** 使用一次性票据完成首次登录密码修改；成功后仍需重新登录。 */
    public synchronized void forceResetPassword(String resetToken, String newPassword)
    {
        if (StringUtils.isEmpty(resetToken))
        {
            throw new ServiceException("改密票据不能为空，请重新验证账号密码");
        }
        validateNewPassword(newPassword);
        String cacheKey = CacheConstants.PWD_RESET_TOKEN_KEY + resetToken;
        String userIdText = redisCache.getCacheObject(cacheKey);
        if (StringUtils.isEmpty(userIdText))
        {
            throw new ServiceException("改密票据已失效，请返回登录页重新验证");
        }
        Long userId;
        try
        {
            userId = Long.valueOf(userIdText);
        }
        catch (NumberFormatException ex)
        {
            redisCache.deleteObject(cacheKey);
            throw new ServiceException("改密票据无效，请返回登录页重新验证");
        }
        SysUser user = userService.selectUserById(userId);
        if (user == null || !UserConstants.NORMAL.equals(user.getStatus()) || !UserConstants.NORMAL.equals(user.getDelFlag()))
        {
            redisCache.deleteObject(cacheKey);
            throw new ServiceException("账号不存在、已停用或已删除");
        }
        if (!isInitialPasswordChangeRequired(user))
        {
            redisCache.deleteObject(cacheKey);
            throw new ServiceException("该账号已完成初始密码修改，请直接登录");
        }
        if (SecurityUtils.matchesPassword(newPassword, user.getPassword()))
        {
            throw new ServiceException("新密码不能与初始密码相同");
        }
        if (!redisCache.deleteObject(cacheKey))
        {
            throw new ServiceException("改密票据已被使用，请返回登录页重新验证");
        }
        if (userService.resetUserPwd(userId, SecurityUtils.encryptPassword(newPassword)) <= 0)
        {
            throw new ServiceException("修改密码失败，请重新验证后再试");
        }
        tokenService.deleteTokensByUserId(userId);
        AsyncManager.me().execute(AsyncFactory.recordLogininfor(user.getUserName(), Constants.LOGIN_SUCCESS,
                "初始密码修改成功"));
    }

    private LoginUser authenticate(String username, String password)
    {
        Authentication authentication;
        try
        {
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
            AuthenticationContextHolder.setContext(authenticationToken);
            authentication = authenticationManager.authenticate(authenticationToken);
        }
        catch (Exception e)
        {
            if (e instanceof BadCredentialsException)
            {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL,
                        MessageUtils.message("user.password.not.match")));
                throw new UserPasswordNotMatchException();
            }
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, e.getMessage()));
            throw new ServiceException(e.getMessage());
        }
        finally
        {
            AuthenticationContextHolder.clearContext();
        }
        return (LoginUser) authentication.getPrincipal();
    }

    private boolean isInitialPasswordChangeRequired(SysUser user)
    {
        Integer enabled = Convert.toInt(configService.selectConfigByKey("sys.account.initPasswordModify"));
        return enabled != null && enabled == 1 && user != null && user.getPwdUpdateDate() == null;
    }

    private void validateNewPassword(String password)
    {
        if (StringUtils.isEmpty(password) || password.length() < NEW_PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            throw new ServiceException("新密码长度必须介于6和20个字符之间");
        }
        String charType = Convert.toStr(configService.selectConfigByKey("sys.account.chrtype"), "0");
        boolean valid;
        String message;
        switch (charType)
        {
            case "1":
                valid = password.matches("^[0-9]+$");
                message = "新密码只能包含数字";
                break;
            case "2":
                valid = password.matches("^[A-Za-z]+$");
                message = "新密码只能包含英文字母";
                break;
            case "3":
                valid = password.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]+$");
                message = "新密码必须同时包含字母和数字";
                break;
            case "4":
                valid = password.matches("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[~!@#$%^&*()\\-=_+])[A-Za-z\\d~!@#$%^&*()\\-=_+]+$");
                message = "新密码必须同时包含字母、数字和特殊字符";
                break;
            default:
                valid = password.chars().noneMatch(ch -> "<>\"'\\|".indexOf(ch) >= 0);
                message = "新密码包含非法字符：< > \" ' \\ |";
                break;
        }
        if (!valid)
        {
            throw new ServiceException(message);
        }
    }

    /**
     * 校验验证码
     * 
     * @param username 用户名
     * @param code 验证码
     * @param uuid 唯一标识
     * @return 结果
     */
    public void validateCaptcha(String username, String code, String uuid)
    {
        boolean captchaEnabled = configService.selectCaptchaEnabled();
        if (captchaEnabled)
        {
            String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + StringUtils.nvl(uuid, "");
            String captcha = redisCache.getCacheObject(verifyKey);
            if (captcha == null)
            {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire")));
                throw new CaptchaExpireException();
            }
            redisCache.deleteObject(verifyKey);
            if (!code.equalsIgnoreCase(captcha))
            {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.error")));
                throw new CaptchaException();
            }
        }
    }

    /**
     * 登录前置校验
     * @param username 用户名
     * @param password 用户密码
     */
    public void loginPreCheck(String username, String password)
    {
        // 用户名或密码为空 错误
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password))
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("not.null")));
            throw new UserNotExistsException();
        }
        // 密码如果不在指定范围内 错误
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
            throw new UserPasswordNotMatchException();
        }
        // 用户名不在指定范围内 错误
        if (username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH)
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
            throw new UserPasswordNotMatchException();
        }
        // IP黑名单校验
        String blackStr = configService.selectConfigByKey("sys.login.blackIPList");
        if (IpUtils.isMatchedIp(blackStr, IpUtils.getIpAddr()))
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("login.blocked")));
            throw new BlackListException();
        }
    }

    /**
     * 记录登录信息
     *
     * @param userId 用户ID
     */
    public void recordLoginInfo(Long userId)
    {
        userService.updateLoginInfo(userId, IpUtils.getIpAddr(), DateUtils.getNowDate());
    }
}
