package back.global.scheduler;

import back.domain.prompt.prompt.service.PromptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SkillsScheduler")
class SkillsSchedulerTest {

    @Mock private PromptService promptService;

    private SkillsScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new SkillsScheduler(promptService);
    }

    @Test
    @DisplayName("triggerSkillsSync calls PromptService#run")
    void triggerSkillsSync_callsPromptServiceRun() {
        scheduler.triggerSkillsSync();

        verify(promptService).run();
    }
}
