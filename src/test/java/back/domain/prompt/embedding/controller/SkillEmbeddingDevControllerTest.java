package back.domain.prompt.embedding.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import back.domain.prompt.embedding.dto.SkillEmbeddingExportDto;
import back.domain.prompt.embedding.service.SkillEmbeddingExportService;

@ExtendWith(MockitoExtension.class)
class SkillEmbeddingDevControllerTest {

    @TempDir
    Path tempDir;

    @Mock
    private SkillEmbeddingExportService skillEmbeddingExportService;

    @Test
    @DisplayName("exportSkillsAsJson은 서비스 결과를 그대로 반환한다")
    void exportSkillsAsJson_returnsServiceResult() {
        SkillEmbeddingDevController controller = new SkillEmbeddingDevController(skillEmbeddingExportService);
        List<SkillEmbeddingExportDto> dtos = List.of(
                SkillEmbeddingExportDto.builder()
                        .skillId(1L)
                        .repositoryId(2L)
                        .name("alpha")
                        .path("skills/alpha.md")
                        .contentMd("alpha content")
                        .contentHash("hash")
                        .build()
        );
        when(skillEmbeddingExportService.getAllSkillsForExport()).thenReturn(dtos);

        ResponseEntity<List<SkillEmbeddingExportDto>> response = controller.exportSkillsAsJson();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(dtos);
    }

    @Test
    @DisplayName("exportSkillsAsFile은 생성된 파일을 다운로드 응답으로 반환한다")
    void exportSkillsAsFile_returnsDownloadResponse() throws Exception {
        SkillEmbeddingDevController controller = new SkillEmbeddingDevController(skillEmbeddingExportService);
        Path file = tempDir.resolve("skills_for_embedding.json");
        Files.writeString(file, "[{\"skill_id\":1}]");
        when(skillEmbeddingExportService.exportAllSkillsToJson()).thenReturn(file);

        ResponseEntity<byte[]> response = controller.exportSkillsAsFile();

        verify(skillEmbeddingExportService).exportAllSkillsToJson();
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("skills_for_embedding.json");
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(new String(response.getBody())).contains("\"skill_id\":1");
    }
}
