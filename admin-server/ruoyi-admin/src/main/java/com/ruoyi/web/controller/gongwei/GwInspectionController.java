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
import com.ruoyi.system.domain.gongwei.GwInspectionRecord;
import com.ruoyi.system.domain.gongwei.GwInspectionTask;
import com.ruoyi.system.service.gongwei.IGwInspectionService;

@RestController
@RequestMapping("/gw/inspection")
public class GwInspectionController extends BaseController {
    @Autowired private IGwInspectionService inspectionService;

    @PreAuthorize("@ss.hasPermi('gw:inspection:task:list')")
    @GetMapping("/task/list")
    public AjaxResult taskList(String status) {
        Long inspectorId = SecurityUtils.isGongweiAdmin() ? null : getUserId();
        return success(inspectionService.selectTaskList(inspectorId, status));
    }

    @PreAuthorize("@ss.hasPermi('gw:inspection:task:add')")
    @Log(title = "卫生检查任务", businessType = BusinessType.INSERT)
    @PostMapping("/task")
    public AjaxResult createTask(@RequestBody GwInspectionTask task) {
        task.setStatus("PENDING");
        task.setCreateBy(getUsername());
        return toAjax(inspectionService.createTask(task));
    }

    @PreAuthorize("@ss.hasPermi('gw:inspection:record:submit')")
    @Log(title = "完成卫生检查任务", businessType = BusinessType.UPDATE)
    @PostMapping("/task/{taskId}/complete")
    public AjaxResult completeTask(@PathVariable Long taskId) {
        return toAjax(inspectionService.completeTask(taskId, getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('gw:inspection:record:list')")
    @GetMapping("/record/list")
    public AjaxResult recordList(String status) {
        Long inspectorId = SecurityUtils.isGongweiAdmin() ? null : getUserId();
        return success(inspectionService.selectRecordList(inspectorId, status));
    }

    @PreAuthorize("@ss.hasPermi('gw:inspection:record:submit')")
    @Log(title = "卫生检查上报", businessType = BusinessType.INSERT)
    @PostMapping("/record")
    public AjaxResult submitRecord(@RequestBody GwInspectionRecord record) { record.setInspectorId(getUserId()); return toAjax(inspectionService.submitRecord(record)); }

    @PreAuthorize("@ss.hasPermi('gw:inspection:record:verify')")
    @Log(title = "卫生检查核实", businessType = BusinessType.UPDATE)
    @PostMapping("/record/verify")
    public AjaxResult verifyRecord(@RequestBody Map<String, String> body) { return toAjax(inspectionService.verifyRecord(Long.valueOf(body.get("recordId")), body.get("status"), body.get("comment"), getUserId())); }
}
