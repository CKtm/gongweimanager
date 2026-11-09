package com.ruoyi.web.controller.gongwei;

import java.util.Locale;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.gongwei.GwPersonImportRow;
import com.ruoyi.system.domain.gongwei.GwUserProfile;
import com.ruoyi.system.mapper.gongwei.GwUserProfileMapper;
import com.ruoyi.system.service.gongwei.IGwPersonImportService;

@RestController
@RequestMapping("/gw/profile")
public class GwUserProfileController extends BaseController {
    @Autowired private GwUserProfileMapper profileMapper;
    @Autowired private IGwPersonImportService personImportService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public AjaxResult me() { return success(profileMapper.selectByUserId(getUserId())); }

    @PreAuthorize("@ss.hasPermi('gw:profile:edit')")
    @Log(title = "人员账号与关系导入", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file) throws Exception {
        List<GwPersonImportRow> rows = new ExcelUtil<GwPersonImportRow>(GwPersonImportRow.class).importExcel(file.getInputStream());
        return success(personImportService.importPeople(rows, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('gw:profile:edit')")
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response) {
        new ExcelUtil<GwPersonImportRow>(GwPersonImportRow.class).importTemplateExcel(response, "人员账号导入数据");
    }

    @PreAuthorize("@ss.hasPermi('gw:profile:list')")
    @GetMapping("/list")
    public AjaxResult list(Long userId, String identityNo, String userType) { return success(profileMapper.selectList(userId, identityNo, userType)); }

    @PreAuthorize("@ss.hasPermi('gw:profile:list')")
    @GetMapping("/options")
    public AjaxResult options(String roleKey) {
        if (!"common".equals(roleKey) && !"mentor".equals(roleKey) && !"inspector".equals(roleKey)) return error("不支持的角色类型");
        return success(profileMapper.selectRoleUsers(roleKey));
    }

    @PreAuthorize("@ss.hasPermi('gw:assignment:list')")
    @GetMapping("/team")
    public AjaxResult team(Long mentorId) {
        Long targetMentorId = SecurityUtils.isGongweiAdmin() ? mentorId : getUserId();
        if (targetMentorId == null) return error("导师不能为空");
        return success(profileMapper.selectStudentsByMentor(targetMentorId));
    }

    @PreAuthorize("@ss.hasPermi('gw:profile:edit')")
    @Log(title = "师生业务档案", businessType = BusinessType.UPDATE)
    @PostMapping
    public AjaxResult save(@RequestBody GwUserProfile profile) {
        if (profile.getUserId() == null || profile.getIdentityNo() == null || profile.getIdentityNo().isBlank() || profile.getUserType() == null) return error("用户、编号和类型不能为空");
        String userType = profile.getUserType().trim().toUpperCase(Locale.ROOT);
        String roleKey = switch (userType) { case "STUDENT" -> "common"; case "MENTOR" -> "mentor"; case "INSPECTOR" -> "inspector"; default -> null; };
        if (roleKey == null) return error("不支持的身份类型");
        if (profileMapper.countActiveUserByRole(profile.getUserId(), roleKey) == 0) return error("所选账号未分配对应系统角色或已停用");
        if ("STUDENT".equals(userType) && profile.getMentorId() != null && profileMapper.countActiveUserByRole(profile.getMentorId(), "mentor") == 0) return error("所选导师账号未分配导师角色或已停用");
        Long identityUserId = profileMapper.selectUserIdByIdentityNo(profile.getIdentityNo().trim());
        if (identityUserId != null && !identityUserId.equals(profile.getUserId())) return error("该学号/工号已被其他账号使用");
        GwUserProfile current = profileMapper.selectByUserId(profile.getUserId());
        if (current != null && current.getUserType() != null && !current.getUserType().equalsIgnoreCase(userType)) {
            if (hasBlockingBusiness(current.getUserId(), current.getUserType())) return error("该人员仍有关联业务，不能变更身份类型");
        }
        if (current != null && "STUDENT".equalsIgnoreCase(current.getUserType())
            && !Objects.equals(current.getMentorId(), profile.getMentorId())
            && profileMapper.countBlockingStudentBusiness(profile.getUserId()) > 0) {
            return error("学生已有工位业务记录，为保证历史归属不能改绑或解绑导师");
        }
        if (!"STUDENT".equals(userType)) profile.setMentorId(null);
        profile.setIdentityNo(profile.getIdentityNo().trim());
        profile.setUserType(userType);
        return toAjax(profileMapper.upsert(profile));
    }

    @PreAuthorize("@ss.hasPermi('gw:profile:edit')")
    @Log(title = "绑定师生关系", businessType = BusinessType.UPDATE)
    @PostMapping("/relation")
    public AjaxResult bind(@RequestBody Map<String, Object> body) {
        Long studentId = toLong(body.get("studentId"));
        Long mentorId = toLong(body.get("mentorId"));
        if (studentId == null || mentorId == null) return error("学生和导师不能为空");
        if (studentId.equals(mentorId)) return error("学生和导师不能是同一账号");
        if (profileMapper.countActiveUserByRole(studentId, "common") == 0) return error("所选学生账号未分配普通角色或已停用");
        if (profileMapper.countActiveUserByRole(mentorId, "mentor") == 0) return error("所选导师账号未分配导师角色或已停用");
        GwUserProfile current = profileMapper.selectByUserId(studentId);
        if (current != null && current.getUserType() != null && !"STUDENT".equalsIgnoreCase(current.getUserType())
            && hasBlockingBusiness(current.getUserId(), current.getUserType())) return error("该人员仍有关联业务，不能改为学生身份");
        if (current != null && current.getMentorId() != null && !current.getMentorId().equals(mentorId)
            && profileMapper.countBlockingStudentBusiness(studentId) > 0) return error("学生已有工位业务记录，为保证历史归属不能更换导师");
        return toAjax(profileMapper.bindMentor(studentId, mentorId));
    }

    @PreAuthorize("@ss.hasPermi('gw:profile:edit')")
    @Log(title = "解除师生关系", businessType = BusinessType.UPDATE)
    @DeleteMapping("/relation/{studentId}")
    public AjaxResult unbind(@PathVariable Long studentId) {
        if (profileMapper.countBlockingStudentBusiness(studentId) > 0) return error("学生已有工位业务记录，为保证历史归属不能解绑导师");
        return toAjax(profileMapper.unbindMentor(studentId));
    }

    private boolean hasBlockingBusiness(Long userId, String userType) {
        return switch (userType.toUpperCase(Locale.ROOT)) {
            case "STUDENT" -> profileMapper.countBlockingStudentBusiness(userId) > 0;
            case "MENTOR" -> profileMapper.countBlockingMentorBusiness(userId) > 0;
            case "INSPECTOR" -> profileMapper.countBlockingInspectorBusiness(userId) > 0;
            default -> false;
        };
    }

    private Long toLong(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return null;
        try { return Long.valueOf(String.valueOf(value)); } catch (NumberFormatException ex) { return null; }
    }
}
