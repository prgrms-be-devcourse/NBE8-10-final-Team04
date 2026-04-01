package back.domain.prompt.embedding.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import back.domain.prompt.embedding.service.SkillChunkImportService;

@ExtendWith(MockitoExtension.class)
class SkillChunkDevControllerTest {

    @Mock
    private SkillChunkImportService skillChunkImportService;

    @Test
    @DisplayName("importJsonl은 서비스 호출 후 성공 메시지를 반환한다")
    void importJsonl_callsServiceAndReturnsMessage() {
        SkillChunkDevController controller = new SkillChunkDevController(skillChunkImportService);

        ResponseEntity<String> response = controller.importJsonl("data/chunks.jsonl");

        verify(skillChunkImportService).importFromJsonl("data/chunks.jsonl");
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("import");
    }
}
