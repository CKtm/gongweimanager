package com.ruoyi.system.service.gongwei;

import java.util.List;
import com.ruoyi.system.domain.gongwei.GwInspectionRecord;
import com.ruoyi.system.domain.gongwei.GwInspectionTask;

public interface IGwInspectionService {
    List<GwInspectionTask> selectTaskList(Long inspectorId, String status);
    int createTask(GwInspectionTask task);
    int completeTask(Long taskId, Long inspectorId);
    List<GwInspectionRecord> selectRecordList(Long inspectorId, String status);
    int submitRecord(GwInspectionRecord record);
    int verifyRecord(Long recordId, String status, String comment, Long operatorId);
}
