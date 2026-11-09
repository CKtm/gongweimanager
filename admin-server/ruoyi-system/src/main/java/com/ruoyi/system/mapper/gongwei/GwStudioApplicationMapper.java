package com.ruoyi.system.mapper.gongwei;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.gongwei.GwStudioApplication;

@Mapper
public interface GwStudioApplicationMapper {
    List<GwStudioApplication> selectList(@Param("status") String status, @Param("mentorId") Long mentorId);
    GwStudioApplication selectForUpdate(Long studioApplicationId);
    int countPending(@Param("mentorId") Long mentorId, @Param("roomId") Long roomId);
    int insertApplication(GwStudioApplication application);
    int review(@Param("studioApplicationId") Long studioApplicationId, @Param("status") String status,
               @Param("comment") String comment, @Param("reviewerId") Long reviewerId);
    int markReturned(@Param("roomId") Long roomId, @Param("mentorId") Long mentorId, @Param("comment") String comment,
                     @Param("reviewerId") Long reviewerId);
    int insertAudit(@Param("businessId") Long businessId, @Param("action") String action,
                    @Param("operatorId") Long operatorId, @Param("roomId") Long roomId,
                    @Param("mentorId") Long mentorId, @Param("reason") String reason);
}
