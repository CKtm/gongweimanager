package com.ruoyi.system.mapper.gongwei;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.gongwei.GwMobileApplication;

@Mapper
public interface GwApplicationMapper {
    List<GwMobileApplication> selectApplicationList(@Param("application") GwMobileApplication application,
                                                     @Param("scope") String scope,
                                                     @Param("userId") Long userId);
    GwMobileApplication selectApplicationById(@Param("applicationId") Long applicationId,
                                              @Param("scope") String scope,
                                              @Param("userId") Long userId);
    GwMobileApplication selectApplicationForUpdate(Long applicationId);
    int countPendingByStudent(Long studentId);
    int insertApplication(GwMobileApplication application);
    int updateMentorReview(@Param("applicationId") Long applicationId,
                           @Param("mentorId") Long mentorId,
                           @Param("status") String status,
                           @Param("comment") String comment);
    int updateAdminReview(@Param("applicationId") Long applicationId,
                          @Param("status") String status,
                          @Param("comment") String comment);
    int updateApplicationStatus(@Param("applicationId") Long applicationId,
                                @Param("status") String status,
                                @Param("workstationId") Long workstationId);
    int insertAssignment(@Param("studentId") Long studentId,
                         @Param("workstationId") Long workstationId,
                         @Param("applicationId") Long applicationId,
                         @Param("operatorId") Long operatorId);
    int countActiveAssignmentByStudent(Long studentId);
}
