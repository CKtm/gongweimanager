package com.ruoyi.system.service.gongwei;

import java.util.List;
import com.ruoyi.system.domain.gongwei.GwReturnApplication;

public interface IGwReturnService {
    List<GwReturnApplication> selectList(Long studentId, Long mentorId, String status);
    int submit(Long assignmentId, Long studentId, String reason, String photoUrls);
    int mentorReview(Long returnId, Long mentorId, String status, String comment);
    int adminReview(Long returnId, String status, String comment, Long operatorId);
    int uploadPhotos(Long returnId, Long studentId, String photoUrls);
    int markCleared(Long returnId, Long studentId);
    void verify(Long returnId, boolean approved, String comment, Long operatorId);
}
