package com.ruoyi.system.mapper.gongwei;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.gongwei.GwAssignment;

@Mapper
public interface GwAssignmentMapper {
    List<GwAssignment> selectAssignmentList(@Param("studentId") Long studentId, @Param("mentorId") Long mentorId, @Param("activeOnly") Boolean activeOnly);
    GwAssignment selectActiveByStudent(Long studentId);
    GwAssignment selectByIdForUpdate(Long assignmentId);
    int insertAssignment(@Param("studentId") Long studentId, @Param("workstationId") Long workstationId, @Param("applicationId") Long applicationId, @Param("termCode") String termCode, @Param("operatorId") Long operatorId);
    int finishAssignment(@Param("assignmentId") Long assignmentId, @Param("operatorId") Long operatorId);
    int insertAudit(@Param("businessId") Long businessId, @Param("action") String action,
                    @Param("operatorId") Long operatorId, @Param("studentId") Long studentId,
                    @Param("beforeWorkstationId") Long beforeWorkstationId,
                    @Param("afterWorkstationId") Long afterWorkstationId,
                    @Param("reason") String reason);
}
