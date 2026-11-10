package com.ruoyi.web.controller.gongwei;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.service.gongwei.IGwStudioApplicationService;

@RestController
@RequestMapping("/gw/studio")
public class GwStudioApplicationController extends BaseController {
    @Autowired private IGwStudioApplicationService studioService;

    @PreAuthorize("@ss.hasPermi('gw:studio:list')")
    @GetMapping("/list")
    public AjaxResult list(String status) {
        Long mentorId = SecurityUtils.isGongweiAdmin() ? null : getUserId();
        return success(studioService.selectList(status, mentorId));
    }

    @PreAuthorize("@ss.hasPermi('gw:studio:apply')")
    @Log(title="工作室申请", businessType=BusinessType.INSERT)
    @PostMapping
    public AjaxResult submit(@RequestBody ApplyBody body) {
        return error("工作室申请已停用，固定工位由管理员直接划分给导师");
    }

    @PreAuthorize("@ss.hasPermi('gw:studio:review')")
    @Log(title="工作室申请审批", businessType=BusinessType.UPDATE)
    @PostMapping("/{studioApplicationId}/review")
    public AjaxResult review(@PathVariable Long studioApplicationId, @RequestBody ReviewBody body) {
        return error("工作室申请审批已停用，请在工位管理中直接划分导师");
    }

    @PreAuthorize("@ss.hasPermi('gw:studio:release')")
    @Log(title="解除工作室归属", businessType=BusinessType.UPDATE)
    @PostMapping("/{roomId}/release")
    public AjaxResult release(@PathVariable Long roomId, @RequestBody ReleaseBody body) {
        if (!SecurityUtils.isGongweiAdmin()) return error("只有管理员可以解除工作室归属");
        return toAjax(studioService.release(roomId, body.reason, getUserId()));
    }

    public static class ApplyBody { public Long roomId; public String reason; }
    public static class ReviewBody { public String status; public String comment; }
    public static class ReleaseBody { public String reason; }
}
