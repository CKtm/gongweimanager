package com.ruoyi.system.mapper.gongwei;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.gongwei.GwWorkstation;

@Mapper
public interface GwWorkstationMapper {
    List<GwWorkstation> selectWorkstationList(GwWorkstation workstation);
    GwWorkstation selectWorkstationById(Long workstationId);
    GwWorkstation selectWorkstationForUpdate(Long workstationId);
    int insertWorkstation(GwWorkstation workstation);
    int updateWorkstation(GwWorkstation workstation);
    int deleteWorkstationById(Long workstationId);
    int countWorkstationReferences(Long workstationId);
    int countPendingWorkstationFlows(Long workstationId);
    int countByRoomAndNo(@Param("roomId") Long roomId, @Param("workstationNo") String workstationNo);
    int updateStatus(@Param("workstationId") Long workstationId, @Param("status") String status);
    int countAvailableFixedByMentor(Long mentorId);
    List<GwWorkstation> selectRoomWorkstationsForUpdate(Long roomId);
    int updateMentor(@Param("workstationId") Long workstationId, @Param("mentorId") Long mentorId, @Param("operator") String operator);
}
