package com.codesquad.autobid.auction.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class AuctionScheduler {

    private static final String CRON_RATE = "0 */15 * * * *";

    @Async
    @Scheduled(cron = CRON_RATE)
    public void openPendingAuctions() {
        log.info("openPendingAuctions: " + LocalDateTime.now());
    }

    @Async
    @Scheduled(cron = CRON_RATE)
    public void closeInProgressAuctions() {
        log.info("closeInProgressAuctions: " + LocalDateTime.now());
    }
}
