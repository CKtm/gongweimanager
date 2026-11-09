package com.ruoyi.web.controller.gongwei;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.ruoyi.system.service.gongwei.IGwReturnService;

@RestController
@RequestMapping("/gw/return")
public class GwReturnController extends BaseController {
    @Autowired private IGwReturnService returnService;

    @PreAuthorize("@ss.hasPermi('gw:return:query')")
    @GetMapping("/list")
    public AjaxResult list(String status) {
        Long studentId = SecurityUtils.isGongweiAdmin() || SecurityUtils.hasRole("mentor") ? null : getUserId();
        Long mentorId = SecurityUtils.hasRole("mentor") && !SecurityUtils.isGongweiAdmin() ? getUserId() : null;
        return success(returnService.selectList(studentId, mentorId, status));
    }

    @PreAuthorize("@ss.hasPermi('gw:return:submit')")
    @Log(title = "工位退还申请", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult submit(@RequestBody Map<String, Object> body) {
        Long assignmentId = toLong(body.get("assignmentId"));
        if (assignmentId == null) return error("工位占用记录不能为空");
        return toAjax(returnService.submit(assignmentId, getUserId(), text(body.get("reason")), text(body.get("photoUrls"))));
    }

    @PreAuthorize("@ss.hasPermi('gw:return:review')")
    @Log(title = "工位退还审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{returnId}/review")
    public AjaxResult review(@PathVariable Long returnId, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (SecurityUtils.hasRole("mentor") && !SecurityUtils.isGongweiAdmin()) return toAjax(returnService.mentorReview(returnId, getUserId(), status, body.get("comment")));
        if (!SecurityUtils.isGongweiAdmin()) return error("只有导师或学院管理员可以审核退还申请");
        return toAjax(returnService.adminReview(returnId, status, body.get("comment"), getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('gw:return:clear')")
    @Log(title = "上传工位清空照片", businessType = BusinessType.UPDATE)
    @PostMapping("/{returnId}/photos")
    public AjaxResult uploadPhotos(@PathVariable Long returnId, @RequestBody Map<String, Object> body) {
        return toAjax(returnService.uploadPhotos(returnId, getUserId(), text(body.get("photoUrls"))));
    }

    @PreAuthorize("@ss.hasPermi('gw:return:clear')")
    @PostMapping("/{returnId}/clear")
    public AjaxResult clear(@PathVariable Long returnId) { return toAjax(returnService.markCleared(returnId, getUserId())); }

    @PreAuthorize("@ss.hasPermi('gw:return:verify')")
    @Log(title = "工位回收核验", businessType = BusinessType.UPDATE)
    @PostMapping("/{returnId}/verify")
    public AjaxResult verify(@PathVariable Long returnId, @RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isGongweiAdmin()) return error("只有学院管理员可以核验回收");
        boolean approved = Boolean.parseBoolean(String.valueOf(body.getOrDefault("approved", Boolean.TRUE)));
        returnService.verify(returnId, approved, String.valueOf(body.getOrDefault("comment", "")), getUserId());
        return success();
    }

    private Long toLong(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return null;
        try { return Long.valueOf(String.valueOf(value)); } catch (NumberFormatException ex) { return null; }
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
