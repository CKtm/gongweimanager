package com.ruoyi.web.controller.gongwei;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.service.gongwei.IGwAssignmentService;

@RestController
@RequestMapping("/gw/assignment")
public class GwAssignmentController extends BaseController {
    @Autowired private IGwAssignmentService assignmentService;

    @PreAuthorize("@ss.hasPermi('gw:assignment:list')")
    @GetMapping("/list")
    public AjaxResult list(Boolean activeOnly) {
        Long studentId = SecurityUtils.isGongweiAdmin() ? null : (SecurityUtils.hasRole("mentor") ? null : getUserId());
        Long mentorId = SecurityUtils.hasRole("mentor") && !SecurityUtils.isGongweiAdmin() ? getUserId() : null;
        return success(assignmentService.selectAssignmentList(studentId, mentorId, activeOnly));
    }

    @PreAuthorize("@ss.hasPermi('gw:assignment:my')")
    @GetMapping("/my")
    public AjaxResult my() { return success(assignmentService.selectAssignmentList(getUserId(), null, true)); }

    @PreAuthorize("@ss.hasPermi('gw:assignment:assign')")
    @Log(title = "固定工位分配", businessType = BusinessType.INSERT)
    @PostMapping("/fixed")
    public AjaxResult assignFixed(@RequestBody AssignBody body) {
        assignmentService.assignFixed(body.studentId, body.workstationId, getUserId(), body.termCode, SecurityUtils.isGongweiAdmin());
        return success();
    }

    @PreAuthorize("@ss.hasPermi('gw:assignment:adjust')")
    @Log(title = "工位调整", businessType = BusinessType.UPDATE)
    @PostMapping("/adjust")
    public AjaxResult adjust(@RequestBody AdjustBody body) {
        assignmentService.adjustAssignment(body.assignmentId, body.targetWorkstationId, body.reason, getUserId(), SecurityUtils.isGongweiAdmin());
        return success();
    }

    @PreAuthorize("@ss.hasPermi('gw:assignment:recall')")
    @Log(title = "主动收回工位", businessType = BusinessType.UPDATE)
    @PostMapping("/recall")
    public AjaxResult recall(@RequestBody RecallBody body) {
        assignmentService.initiateRecall(body.assignmentId, body.reason, getUserId(), SecurityUtils.isGongweiAdmin());
        return success();
    }

    public static class AssignBody { public Long studentId; public Long workstationId; public String termCode; }
    public static class AdjustBody { public Long assignmentId; public Long targetWorkstationId; public String reason; }
    public static class RecallBody { public Long assignmentId; public String reason; }
}
