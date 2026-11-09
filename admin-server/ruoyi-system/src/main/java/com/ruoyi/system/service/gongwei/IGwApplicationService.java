package com.ruoyi.system.service.gongwei;

import java.util.List;
import com.ruoyi.system.domain.gongwei.GwMobileApplication;

public interface IGwApplicationService {
    List<GwMobileApplication> selectApplicationList(GwMobileApplication application, String scope, Long userId);
    GwMobileApplication selectApplicationById(Long applicationId, String scope, Long userId);
    int submitApplication(GwMobileApplication application);
    int mentorReview(Long applicationId, Long mentorId, String status, String comment);
    int adminReview(Long applicationId, String status, String comment, Long workstationId, Long operatorId);
    void assignWorkstation(Long applicationId, Long workstationId, Long operatorId);
}
