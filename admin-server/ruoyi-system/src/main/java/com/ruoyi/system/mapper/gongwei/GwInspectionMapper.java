package com.ruoyi.system.mapper.gongwei;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.gongwei.GwInspectionRecord;
import com.ruoyi.system.domain.gongwei.GwInspectionTask;

@Mapper
public interface GwInspectionMapper {
    List<GwInspectionTask> selectTaskList(@Param("inspectorId") Long inspectorId, @Param("status") String status);
    GwInspectionTask selectTaskById(Long taskId);
    GwInspectionTask selectTaskForUpdate(Long taskId);
    int insertTask(GwInspectionTask task);
    int updateTaskStatus(@Param("taskId") Long taskId, @Param("fromStatus") String fromStatus,
                         @Param("toStatus") String toStatus);
    List<GwInspectionRecord> selectRecordList(@Param("inspectorId") Long inspectorId, @Param("status") String status);
    GwInspectionRecord selectRecordById(Long recordId);
    int countRecordTarget(@Param("taskId") Long taskId, @Param("roomId") Long roomId, @Param("workstationId") Long workstationId);
    int countRecords(Long taskId);
    int countNotArchivedRecords(Long taskId);
    int insertRecord(GwInspectionRecord record);
    int updateRecordVerify(@Param("recordId") Long recordId, @Param("fromStatus") String fromStatus,
                           @Param("toStatus") String toStatus, @Param("comment") String comment,
                           @Param("operatorId") Long operatorId);
}
