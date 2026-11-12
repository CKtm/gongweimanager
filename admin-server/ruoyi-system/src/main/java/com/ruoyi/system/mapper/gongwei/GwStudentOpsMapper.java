package com.ruoyi.system.mapper.gongwei;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GwStudentOpsMapper {
    Long lockStudent(Long userId);
    Map<String, Object> activeAssignment(Long workstationId);
    List<Map<String, Object>> scores(Long studentId);
    Map<String, Object> scoreForUpdate(Long recordId);
    int updateScore(@Param("id") Long id, @Param("score") int score, @Param("grade") String grade, @Param("comment") String comment);
    List<Map<String, Object>> penalties(Map<String, Object> scope);
    Map<String, Object> penalty(Long penaltyId);
    Map<String, Object> penaltyForUpdate(Long penaltyId);
    Map<String, Object> activePenalty(Long workstationId);
    int insertPenalty(Map<String, Object> penalty);
    int closePenalty(@Param("id") Long id, @Param("status") String status, @Param("reason") String reason);
    int updateDeadline(@Param("id") Long id, @Param("until") Date until);
    List<Long> duePenalties();
    int insertMessage(Map<String, Object> message);
    List<Map<String, Object>> messages(Long userId);
    int unread(Long userId);
    int readMessage(@Param("id") Long id, @Param("userId") Long userId);
    int readAllMessages(Long userId);
    List<Long> administratorIds();
    int pendingAppeals(Map<String, Object> scope);
    List<Map<String, Object>> appeals(Map<String, Object> scope);
    Map<String, Object> appealForUpdate(Long appealId);
    int insertAppeal(Map<String, Object> appeal);
    int mentorReview(Map<String, Object> appeal);
    int adminReview(Map<String, Object> appeal);
    Map<String, Object> application(Long applicationId);
    Map<String, Object> returnApplication(Long returnId);
}
