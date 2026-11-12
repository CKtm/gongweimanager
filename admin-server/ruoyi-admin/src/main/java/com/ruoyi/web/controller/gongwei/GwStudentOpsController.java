package com.ruoyi.web.controller.gongwei;

import java.util.Date;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.enums.BusinessType;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.mapper.gongwei.GwStudentOpsMapper;
import com.ruoyi.system.mapper.gongwei.GwUserProfileMapper;
import com.ruoyi.system.service.gongwei.GwStudentOpsService;

@RestController
@RequestMapping("/gw")
@PreAuthorize("isAuthenticated()")
public class GwStudentOpsController extends BaseController {
    @Autowired private GwStudentOpsMapper mapper;
    @Autowired private GwStudentOpsService service;
    @Autowired private GwUserProfileMapper profiles;

    private Map<String, Object> scope() {
        Map<String, Object> scope = new HashMap<>();
        if (SecurityUtils.isGongweiAdmin()) return scope;
        if (SecurityUtils.hasRole("mentor")) scope.put("mentorId", getUserId());
        else if (SecurityUtils.hasRole("common")) scope.put("studentId", getUserId());
        else throw new AccessDeniedException("当前角色不能查看学生记录");
        return scope;
    }

    @GetMapping("/lifecycle/scores")
    public TableDataInfo scores(Long studentId) {
        Long target = studentId == null ? getUserId() : studentId;
        boolean ownStudent = SecurityUtils.hasRole("mentor") && getUserId().equals(profiles.selectMentorId(target));
        boolean ownRecord = SecurityUtils.hasRole("common") && getUserId().equals(target);
        if (!SecurityUtils.isGongweiAdmin() && !ownStudent && !ownRecord) throw new AccessDeniedException("只能查看本人或自己学生的评分");
        startPage();
        return getDataTable(mapper.scores(target));
    }

    @GetMapping("/lifecycle/penalties")
    public TableDataInfo penalties() {
        Map<String, Object> scope = scope();
        startPage();
        return getDataTable(mapper.penalties(scope));
    }

    @PostMapping("/lifecycle/penalties/{id}/deadline")
    @PreAuthorize("@ss.hasPermi('gw:workstation:edit')")
    @Log(title = "调整处罚期限", businessType = BusinessType.UPDATE)
    public AjaxResult deadline(@PathVariable Long id, @RequestBody DeadlineBody body) {
        if (!SecurityUtils.isGongweiAdmin()) throw new AccessDeniedException("仅管理员可调整处罚期限");
        if (body == null) throw new ServiceException("请选择截止时间");
        service.updateDeadline(id, body.until);
        return success();
    }

    @GetMapping("/messages")
    public TableDataInfo messages() {
        startPage();
        return getDataTable(mapper.messages(getUserId()));
    }

    @GetMapping("/messages/unread")
    public AjaxResult unread() { return success(mapper.unread(getUserId())); }

    @PostMapping("/messages/{id}/read")
    public AjaxResult read(@PathVariable Long id) {
        if (mapper.readMessage(id, getUserId()) == 0) throw new AccessDeniedException("消息不存在或不属于本人");
        return success();
    }

    @PostMapping("/messages/read-all")
    public AjaxResult readAll() { return success(mapper.readAllMessages(getUserId())); }

    @GetMapping("/appeals/pending-count")
    public AjaxResult pendingAppeals() {
        boolean admin = SecurityUtils.isGongweiAdmin();
        if (!admin && !SecurityUtils.hasRole("mentor")) return success((Object) 0);
        Map<String, Object> filter = scope();
        filter.put("status", admin ? "PENDING_ADMIN" : "PENDING_MENTOR");
        return success((Object) mapper.pendingAppeals(filter));
    }

    @GetMapping("/appeals")
    public TableDataInfo appeals() {
        Map<String, Object> scope = scope();
        startPage();
        return getDataTable(mapper.appeals(scope));
    }

    @PostMapping("/appeals")
    @Log(title = "学生申诉", businessType = BusinessType.INSERT)
    public AjaxResult submitAppeal(@RequestBody AppealBody body) {
        if (body == null || body.targetId == null) throw new ServiceException("请选择申诉对象");
        service.submitAppeal(getUserId(), body.targetType, body.targetId, body.reason);
        return success();
    }

    @PostMapping("/appeals/{id}/review")
    @Log(title = "学生申诉审核", businessType = BusinessType.UPDATE)
    public AjaxResult review(@PathVariable Long id, @RequestBody ReviewBody body) {
        if (body == null) throw new ServiceException("请填写审核结果");
        boolean admin = SecurityUtils.isGongweiAdmin();
        if (!admin && !SecurityUtils.hasRole("mentor")) throw new AccessDeniedException("仅导师和管理员可审核申诉");
        service.reviewAppeal(id, getUserId(), admin, body.decision, body.comment, body.revisedScore);
        return success();
    }

    public static class DeadlineBody {
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        public Date until;
    }
    public static class AppealBody {
        public String targetType;
        public Long targetId;
        public String reason;
    }
    public static class ReviewBody {
        public String decision;
        public String comment;
        public Integer revisedScore;
    }
}
