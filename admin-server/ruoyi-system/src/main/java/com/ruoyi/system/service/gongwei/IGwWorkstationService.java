package com.ruoyi.system.service.gongwei;

import java.util.List;
import com.ruoyi.system.domain.gongwei.GwWorkstation;

public interface IGwWorkstationService {
    List<GwWorkstation> selectWorkstationList(GwWorkstation workstation);
    GwWorkstation selectWorkstationById(Long workstationId);
    int insertWorkstation(GwWorkstation workstation);
    int createWorkstations(Long roomId, Integer count, String operator);
    int updateWorkstation(GwWorkstation workstation);
    int assignMentor(List<Long> workstationIds, Long mentorId, String operator);
    int deleteWorkstationById(Long workstationId);
    int deleteWorkstations(List<Long> workstationIds);
    int changeWorkstationStatus(List<Long> workstationIds, String status, String operator);
    String importWorkstations(List<GwWorkstation> workstations, String operator);
}
