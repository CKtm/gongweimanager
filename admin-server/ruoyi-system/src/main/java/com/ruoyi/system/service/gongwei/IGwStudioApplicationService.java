package com.ruoyi.system.service.gongwei;

import java.util.List;
import com.ruoyi.system.domain.gongwei.GwStudioApplication;

public interface IGwStudioApplicationService {
    List<GwStudioApplication> selectList(String status, Long mentorId);
    int submit(Long mentorId, Long roomId, String reason);
    int review(Long studioApplicationId, String status, String comment, Long reviewerId);
    int release(Long roomId, String reason, Long operatorId);
}
