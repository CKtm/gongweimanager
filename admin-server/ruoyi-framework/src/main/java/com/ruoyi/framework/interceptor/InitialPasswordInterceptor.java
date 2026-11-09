package com.ruoyi.framework.interceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.core.text.Convert;
import com.ruoyi.system.service.ISysConfigService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** 未修改初始密码前禁止访问工位业务接口。 */
@Component
public class InitialPasswordInterceptor implements HandlerInterceptor {
    @Autowired private ISysConfigService configService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) return true;
        Integer enabled = Convert.toInt(configService.selectConfigByKey("sys.account.initPasswordModify"));
        if (enabled != null && enabled == 1 && loginUser.getUser() != null && loginUser.getUser().getPwdUpdateDate() == null) {
            ServletUtils.renderString(response, JSON.toJSONString(AjaxResult.error(403, "首次登录必须先修改初始密码")));
            return false;
        }
        return true;
    }
}
