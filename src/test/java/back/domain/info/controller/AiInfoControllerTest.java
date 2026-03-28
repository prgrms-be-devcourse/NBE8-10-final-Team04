package back.domain.info.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import back.domain.info.service.AiInfoService;
import back.domain.info.service.StatService;
import back.global.response.RsData;

class AiInfoControllerTest {

    private AiInfoService aiInfoService;

    private StatService statService;

    private AiInfoController aiInfoController;

    @BeforeEach
    void setUp() {
        aiInfoService = mock(AiInfoService.class);
        statService = mock(StatService.class);
        aiInfoController = new AiInfoController(aiInfoService, statService);
    }

    @Test
    @DisplayName("run 호출 시 두 서비스를 실행하고 성공 응답을 반환한다")
    void run_returnsSuccessResponse() throws InterruptedException {
        ResponseEntity<RsData<Void>> response = aiInfoController.run();

        verify(aiInfoService).run();
        verify(statService).run();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isNull();
        assertThat(response.getBody().message()).isNotBlank();
    }
}
