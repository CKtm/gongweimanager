package com.ruoyi.system.service.gongwei;

import java.util.List;
import com.ruoyi.system.domain.gongwei.GwAssignment;

public interface IGwAssignmentService {
    List<GwAssignment> selectAssignmentList(Long studentId, Long mentorId, Boolean activeOnly);
    int batchAssignFixed(List<com.ruoyi.system.domain.gongwei.GwFixedAllocation> allocations, Long operatorId, String termCode, boolean admin);
    int finishAssignment(Long assignmentId, Long operatorId);
    void assignFixed(Long studentId, Long workstationId, Long operatorId, String termCode, boolean admin);
    void adjustAssignment(Long assignmentId, Long targetWorkstationId, String reason, Long operatorId, boolean admin);
    void initiateRecall(Long assignmentId, String reason, Long operatorId, boolean admin);
}
