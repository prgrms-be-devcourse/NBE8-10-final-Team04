package back.global.scheduler;

import back.domain.info.service.AiInfoService;
import back.domain.info.service.ModelBenchmarkService;
import back.domain.info.service.UpdateRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InfoDataScheduler {

    private static final String KST_ZONE = "Asia/Seoul";

    private final AiInfoService aiInfoService;
    private final ModelBenchmarkService modelBenchmarkService;
    private final UpdateRequestService updateRequestService;

    @Scheduled(
            cron = "${app.schedulers.info-data.ai-info.crons[0]}",
            zone = KST_ZONE
    )
    public void triggerAiInfo() {
        log.info("[InfoDataScheduler#triggerAiInfo] 동기화 시작");
        runSafely("triggerAiInfo", aiInfoService::getAiInfo);
    }

    @Scheduled(
            cron = "${app.schedulers.info-data.model-benchmark.crons[0]}",
            zone = KST_ZONE
    )
    public void triggerModelBenchmark() {
        log.info("[InfoDataScheduler#triggerModelBenchmark] 동기화 시작");
        runSafely("triggerModelBenchmark", modelBenchmarkService::getModelBenchmark);
    }

    @Scheduled(
            cron = "${app.schedulers.info-data.update-request.crons[0]}",
            zone = KST_ZONE
    )
    public void triggerUpdateRequest() {
        log.info("[InfoDataScheduler#triggerUpdateRequest] 동기화 시작");
        runSafely("triggerUpdateRequest", updateRequestService::getUpdateRequest);
    }

    private void runSafely(String triggerName, Runnable task) {
        try {
            task.run();
            log.info("[InfoDataScheduler#{}] 동기화 성공", triggerName);
        } catch (Exception e) {
            log.error("[InfoDataScheduler#{}] 동기화 실패", triggerName, e);
        }
    }
}