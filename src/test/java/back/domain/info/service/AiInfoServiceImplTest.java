package back.domain.info.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import back.domain.info.entity.AiModel;
import back.domain.info.entity.AiModelFamily;
import back.domain.info.entity.AiVendor;
import back.domain.info.repository.AiVendorRepository;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class AiInfoServiceImplTest {

    @TempDir
    Path tempDir;

    @Autowired
    private AiVendorRepository aiVendorRepository;

    @Autowired
    private AiInfoServiceImpl aiInfoService;

    @BeforeEach
    void setUp() {
        aiVendorRepository.deleteAll();
    }

    @Test
    @DisplayName("JSON 데이터를 벤더-패밀리-모델 엔티티로 변환해 저장한다")
    void run_savesVendorHierarchyFromJson() throws IOException {
        Path jsonFile = writeJson(
                """
                [
                  {
                    "name": "OpenAI",
                    "official_url": "https://openai.com",
                    "is_active": true,
                    "is_deprecated": false,
                    "families": [
                      {
                        "family_name": "GPT-4.1",
                        "common_description": "Flagship family",
                        "models": [
                          {
                            "model_name": "GPT-4.1",
                            "api_id": "gpt-4.1",
                            "context_window": 128000,
                            "max_output_tokens": 4096,
                            "release_date": "2025-01-10",
                            "is_preview": false,
                            "model_image_url": "https://example.com/gpt-4.1.png",
                            "input_price": 2.50,
                            "output_price": 10.00,
                            "input_modalities": ["text", "image"],
                            "output_modalities": ["text"]
                          }
                        ]
                      }
                    ]
                  }
                ]
                """
        );
        ReflectionTestUtils.setField(aiInfoService, "jsonFilePath", jsonFile.toString());

        aiInfoService.run();

        List<AiVendor> savedVendors = aiVendorRepository.findAll();
        assertThat(savedVendors).hasSize(1);

        AiVendor vendor = savedVendors.getFirst();
        assertThat(vendor.getName()).isEqualTo("OpenAI");
        assertThat(vendor.getOfficialUrl()).isEqualTo("https://openai.com");
        assertThat(vendor.getIsActive()).isTrue();
        assertThat(vendor.getIsDeprecated()).isFalse();
        assertThat(vendor.getModelFamilies()).hasSize(1);

        AiModelFamily family = vendor.getModelFamilies().getFirst();
        assertThat(family.getVendor()).isSameAs(vendor);
        assertThat(family.getFamilyName()).isEqualTo("GPT-4.1");
        assertThat(family.getCommonDescription()).isEqualTo("Flagship family");
        assertThat(family.getModels()).hasSize(1);

        AiModel model = family.getModels().getFirst();
        assertThat(model.getFamily()).isSameAs(family);
        assertThat(model.getModelName()).isEqualTo("GPT-4.1");
        assertThat(model.getApiId()).isEqualTo("gpt-4.1");
        assertThat(model.getReleaseDate()).isEqualTo(LocalDate.of(2025, 1, 10));
        assertThat(model.getInputModalities()).containsExactly("text", "image");
        assertThat(model.getOutputModalities()).containsExactly("text");
    }

    @Test
    @DisplayName("잘못된 날짜와 null 모달리티는 null 날짜와 빈 리스트로 처리한다")
    void run_handlesInvalidDateAndNullModalities() throws IOException {
        Path jsonFile = writeJson(
                """
                [
                  {
                    "name": "Anthropic",
                    "official_url": "https://anthropic.com",
                    "is_active": true,
                    "is_deprecated": false,
                    "families": [
                      {
                        "family_name": "Claude",
                        "common_description": "Helpful models",
                        "models": [
                          {
                            "model_name": "Claude Sonnet",
                            "api_id": "claude-sonnet",
                            "release_date": "not-a-date",
                            "is_preview": true,
                            "input_modalities": null,
                            "output_modalities": null
                          }
                        ]
                      }
                    ]
                  }
                ]
                """
        );
        ReflectionTestUtils.setField(aiInfoService, "jsonFilePath", jsonFile.toString());

        aiInfoService.run();

        AiModel model = aiVendorRepository.findAll()
                .getFirst()
                .getModelFamilies()
                .getFirst()
                .getModels()
                .getFirst();

        assertThat(model.getReleaseDate()).isNull();
        assertThat(model.getInputModalities()).isEmpty();
        assertThat(model.getOutputModalities()).isEmpty();
    }

    @Test
    @DisplayName("JSON 파일을 읽지 못하면 저장하지 않는다")
    void run_whenJsonFileMissing_doesNotSave() {
        ReflectionTestUtils.setField(
                aiInfoService,
                "jsonFilePath",
                tempDir.resolve("missing-ai-info.json").toString()
        );

        aiInfoService.run();

        assertThat(aiVendorRepository.findAll()).isEmpty();
    }

    private Path writeJson(String json) throws IOException {
        Path jsonFile = tempDir.resolve("ai-info.json");
        Files.writeString(jsonFile, json);
        return jsonFile;
    }
}
