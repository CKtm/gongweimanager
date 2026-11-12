package com.ruoyi.web.controller.gongwei;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.ruoyi.system.mapper.gongwei.GwStudentOpsMapper;
import com.ruoyi.system.service.gongwei.GwStudentOpsService;

@Component
@EnableScheduling
public class GwPenaltyExpiryJob {
    private static final Logger LOG = LoggerFactory.getLogger(GwPenaltyExpiryJob.class);
    @Autowired private GwStudentOpsMapper mapper;
    @Autowired private GwStudentOpsService service;

    @Scheduled(fixedDelay = 30000, initialDelay = 30000)
    public void restoreExpiredPenalties() {
        for (Long id : mapper.duePenalties()) {
            try {
                service.expirePenalty(id);
            } catch (Exception exception) {
                LOG.error("处罚 {} 到期恢复失败，将在下一轮重试", id, exception);
            }
        }
    }
}
