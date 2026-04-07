package back.global.scheduler;

import back.domain.prompt.prompt.service.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SkillsScheduler {

    private static final String KST_ZONE = "Asia/Seoul";

    private final PromptService promptService;

    @Scheduled(
            cron = "${app.schedulers.skills.crons[0]}",
            zone = KST_ZONE
    )
    public void triggerSkillsSync() {
        log.info("[SkillsScheduler#triggerSkillsSync] 스킬 동기화 시작");
        promptService.run();
    }
}
