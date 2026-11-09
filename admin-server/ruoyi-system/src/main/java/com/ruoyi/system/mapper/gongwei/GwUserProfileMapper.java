package com.ruoyi.system.mapper.gongwei;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.gongwei.GwUserProfile;

@Mapper
public interface GwUserProfileMapper {
    Long selectMentorId(Long userId);
    GwUserProfile selectByUserId(Long userId);
    Long selectUserIdByIdentityNo(String identityNo);
    List<GwUserProfile> selectList(@Param("userId") Long userId, @Param("identityNo") String identityNo, @Param("userType") String userType);
    List<GwUserProfile> selectRoleUsers(@Param("roleKey") String roleKey);
    List<GwUserProfile> selectStudentsByMentor(@Param("mentorId") Long mentorId);
    int countActiveUserByRole(@Param("userId") Long userId, @Param("roleKey") String roleKey);
    int countBlockingStudentBusiness(Long userId);
    int countBlockingMentorBusiness(Long userId);
    int countBlockingInspectorBusiness(Long userId);
    int upsert(GwUserProfile profile);
    int bindMentor(@Param("studentId") Long studentId, @Param("mentorId") Long mentorId);
    int unbindMentor(@Param("studentId") Long studentId);
}
