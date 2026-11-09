package com.ruoyi.system.mapper.gongwei;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.gongwei.GwReturnApplication;

@Mapper
public interface GwReturnMapper {
    List<GwReturnApplication> selectList(@Param("studentId") Long studentId, @Param("mentorId") Long mentorId, @Param("status") String status);
    GwReturnApplication selectByIdForUpdate(Long returnId);
    int countPendingByAssignment(Long assignmentId);
    int insertApplication(GwReturnApplication application);
    int updateStatus(@Param("returnId") Long returnId, @Param("fromStatus") String fromStatus, @Param("toStatus") String toStatus, @Param("comment") String comment, @Param("mentorId") Long mentorId);
    int completeAfterAdminReview(@Param("returnId") Long returnId, @Param("comment") String comment, @Param("operatorId") Long operatorId);
    int updatePhotos(@Param("returnId") Long returnId, @Param("studentId") Long studentId,
                     @Param("toStatus") String toStatus, @Param("photoUrls") String photoUrls);
    int markCleared(Long returnId);
    int resetClearance(@Param("returnId") Long returnId, @Param("comment") String comment, @Param("operatorId") Long operatorId);
    int finishReturn(@Param("returnId") Long returnId, @Param("assignmentId") Long assignmentId, @Param("operatorId") Long operatorId, @Param("comment") String comment);
    int insertAudit(@Param("returnId") Long returnId, @Param("action") String action,
                    @Param("operatorId") Long operatorId, @Param("beforeStatus") String beforeStatus,
                    @Param("afterStatus") String afterStatus, @Param("reason") String reason);
}
