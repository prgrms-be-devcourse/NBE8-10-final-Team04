package back.global.scheduler;

import back.domain.info.service.AiInfoService;
import back.domain.info.service.ModelBenchmarkService;
import back.domain.info.service.UpdateRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("InfoDataScheduler")
class InfoDataSchedulerTest {

    @Mock private AiInfoService aiInfoService;
    @Mock private ModelBenchmarkService modelBenchmarkService;
    @Mock private UpdateRequestService updateRequestService;

    private InfoDataScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new InfoDataScheduler(aiInfoService, modelBenchmarkService, updateRequestService);
    }

    @Nested
    @DisplayName("triggerAiInfo()")
    class TriggerAiInfo {

        @Test
        @DisplayName("calls AiInfoService#getAiInfo")
        void callsAiInfoService() {
            scheduler.triggerAiInfo();

            verify(aiInfoService).getAiInfo();
        }

        @Test
        @DisplayName("swallows exception and keeps scheduler flow")
        void swallowsException() {
            doThrow(new RuntimeException("boom")).when(aiInfoService).getAiInfo();

            assertThatNoException().isThrownBy(() -> scheduler.triggerAiInfo());
            verify(aiInfoService).getAiInfo();
        }
    }

    @Nested
    @DisplayName("triggerModelBenchmark()")
    class TriggerModelBenchmark {

        @Test
        @DisplayName("calls ModelBenchmarkService#getModelBenchmark")
        void callsModelBenchmarkService() {
            scheduler.triggerModelBenchmark();

            verify(modelBenchmarkService).getModelBenchmark();
        }

        @Test
        @DisplayName("swallows exception and keeps scheduler flow")
        void swallowsException() {
            doThrow(new RuntimeException("boom")).when(modelBenchmarkService).getModelBenchmark();

            assertThatNoException().isThrownBy(() -> scheduler.triggerModelBenchmark());
            verify(modelBenchmarkService).getModelBenchmark();
        }
    }

    @Nested
    @DisplayName("triggerUpdateRequest()")
    class TriggerUpdateRequest {

        @Test
        @DisplayName("calls UpdateRequestService#getUpdateRequest")
        void callsUpdateRequestService() {
            scheduler.triggerUpdateRequest();

            verify(updateRequestService).getUpdateRequest();
        }

        @Test
        @DisplayName("swallows exception and keeps scheduler flow")
        void swallowsException() {
            doThrow(new RuntimeException("boom")).when(updateRequestService).getUpdateRequest();

            assertThatNoException().isThrownBy(() -> scheduler.triggerUpdateRequest());
            verify(updateRequestService).getUpdateRequest();
        }
    }
}
