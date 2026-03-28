package back.domain.info.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import back.domain.info.service.AiInfoServiceImpl;
import back.domain.info.service.StatServiceImpl;
import back.global.response.RsData;

@ActiveProfiles("test")
@SpringBootTest
class AiInfoControllerTest {

    @MockitoBean
    private AiInfoServiceImpl aiInfoService;

    @MockitoBean
    private StatServiceImpl statService;

    @Autowired
    private AiInfoController aiInfoController;

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
