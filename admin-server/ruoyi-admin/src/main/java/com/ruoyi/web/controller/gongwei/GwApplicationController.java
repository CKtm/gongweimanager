package com.ruoyi.web.controller.gongwei;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.gongwei.GwMobileApplication;
import com.ruoyi.system.service.gongwei.IGwApplicationService;

@RestController
@RequestMapping("/gw/application")
public class GwApplicationController extends BaseController {
    @Autowired
    private IGwApplicationService applicationService;

    @PreAuthorize("@ss.hasPermi('gw:application:list')")
    @GetMapping("/list")
    public AjaxResult list(GwMobileApplication application) {
        String scope = SecurityUtils.isGongweiAdmin() ? "ADMIN" : (SecurityUtils.hasRole("mentor") ? "MENTOR" : "SELF");
        if ("SELF".equals(scope)) {
            application.setStudentId(getUserId());
        }
        List<GwMobileApplication> rows = applicationService.selectApplicationList(application, scope, getUserId());
        return success(rows);
    }

    @PreAuthorize("@ss.hasPermi('gw:application:query')")
    @GetMapping("/{applicationId}")
    public AjaxResult getInfo(@PathVariable Long applicationId) {
        String scope = SecurityUtils.isGongweiAdmin() ? "ADMIN" : (SecurityUtils.hasRole("mentor") ? "MENTOR" : "SELF");
        GwMobileApplication application = applicationService.selectApplicationById(applicationId, scope, getUserId());
        return application == null ? error("申请不存在或无权查看") : success(application);
    }

    @PreAuthorize("@ss.hasPermi('gw:application:add')")
    @Log(title = "流动工位申请", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult submit(@Validated @RequestBody GwMobileApplication application) {
        application.setStudentId(getUserId());
        return toAjax(applicationService.submitApplication(application));
    }

    @PreAuthorize("@ss.hasPermi('gw:application:mentorReview')")
    @Log(title = "流动工位导师初审", businessType = BusinessType.UPDATE)
    @PostMapping("/{applicationId}/mentor-review")
    public AjaxResult mentorReview(@PathVariable Long applicationId, @RequestBody Map<String, String> body) {
        return toAjax(applicationService.mentorReview(applicationId, getUserId(), body.get("status"), body.get("comment")));
    }

    @PreAuthorize("@ss.hasPermi('gw:application:adminReview')")
    @Log(title = "流动工位管理员终审", businessType = BusinessType.UPDATE)
    @PostMapping("/{applicationId}/admin-review")
    public AjaxResult adminReview(@PathVariable Long applicationId, @RequestBody AdminReviewBody body) {
        return toAjax(applicationService.adminReview(applicationId, body.status, body.comment, body.workstationId, getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('gw:application:assign')")
    @Log(title = "流动工位分配", businessType = BusinessType.UPDATE)
    @PostMapping("/{applicationId}/assign")
    public AjaxResult assign(@PathVariable Long applicationId, @RequestBody Map<String, Long> body) {
        applicationService.assignWorkstation(applicationId, body.get("workstationId"), getUserId());
        return success();
    }

    public static class AdminReviewBody { public String status; public String comment; public Long workstationId; }
}
