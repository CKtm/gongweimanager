package com.ruoyi.web.controller.gongwei;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
import com.ruoyi.system.domain.gongwei.GwWorkstation;
import com.ruoyi.system.service.gongwei.IGwWorkstationService;

@RestController
@RequestMapping("/gw/workstation")
public class GwWorkstationController extends BaseController {
    @Autowired
    private IGwWorkstationService workstationService;

    @PreAuthorize("@ss.hasPermi('gw:workstation:list')")
    @GetMapping("/list")
    public AjaxResult list(GwWorkstation workstation) {
        List<GwWorkstation> rows = workstationService.selectWorkstationList(workstation);
        if (!SecurityUtils.isGongweiAdmin() && !SecurityUtils.hasRole("inspector")) rows.forEach(item -> { item.setStudentId(null); item.setStudentName(null); });
        return success(rows);
    }

    @PreAuthorize("@ss.hasPermi('gw:workstation:query')")
    @GetMapping("/{workstationId}")
    public AjaxResult getInfo(@PathVariable Long workstationId) {
        return success(workstationService.selectWorkstationById(workstationId));
    }

    @PreAuthorize("@ss.hasPermi('gw:workstation:add')")
    @Log(title = "工位管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody GwWorkstation workstation) {
        if (!SecurityUtils.isGongweiAdmin()) throw new com.ruoyi.common.exception.ServiceException("仅管理员可以维护工位与导师归属");
        workstation.setCreateBy(getUsername());
        return toAjax(workstationService.insertWorkstation(workstation));
    }

    @PreAuthorize("@ss.hasPermi('gw:workstation:add')")
    @Log(title = "工位批量导入", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file) throws Exception {
        if (!SecurityUtils.isGongweiAdmin()) throw new com.ruoyi.common.exception.ServiceException("仅管理员可以维护工位与导师归属");
        ExcelUtil<GwWorkstation> util = new ExcelUtil<>(GwWorkstation.class);
        List<GwWorkstation> rows = util.importExcel(file.getInputStream());
        return success(workstationService.importWorkstations(rows, getUsername()));
    }

    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response) {
        new ExcelUtil<>(GwWorkstation.class).importTemplateExcel(response, "工位导入数据");
    }

    @PreAuthorize("@ss.hasPermi('gw:workstation:edit')")
    @Log(title = "工位管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody GwWorkstation workstation) {
        if (!SecurityUtils.isGongweiAdmin()) throw new com.ruoyi.common.exception.ServiceException("仅管理员可以维护工位与导师归属");
        workstation.setUpdateBy(getUsername());
        return toAjax(workstationService.updateWorkstation(workstation));
    }

    public static class MentorAllocation {
        public List<Long> workstationIds;
        public Long mentorId;
    }

    @PreAuthorize("@ss.hasPermi('gw:workstation:edit')")
    @Log(title = "固定工位划分导师", businessType = BusinessType.UPDATE)
    @PutMapping("/mentor")
    public AjaxResult assignMentor(@RequestBody MentorAllocation allocation) {
        if (!SecurityUtils.isGongweiAdmin()) throw new com.ruoyi.common.exception.ServiceException("仅管理员可以划分工位归属");
        return toAjax(workstationService.assignMentor(allocation.workstationIds, allocation.mentorId, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('gw:workstation:remove')")
    @Log(title = "工位管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{workstationId}")
    public AjaxResult remove(@PathVariable Long workstationId) {
        return toAjax(workstationService.deleteWorkstationById(workstationId));
    }
}
