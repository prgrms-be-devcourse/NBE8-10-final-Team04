package back.domain.info.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import back.domain.info.entity.AiModel;
import back.domain.info.entity.AiModelFamily;
import back.domain.info.entity.AiVendor;
import back.domain.info.mapper.AiModelMapper;
import back.domain.info.repository.AiModelFamilyRepository;
import back.domain.info.repository.AiModelRepository;
import back.domain.info.repository.AiVendorRepository;
import tools.jackson.databind.ObjectMapper;

class AiInfoServiceImplTest {

    @TempDir
    Path tempDir;

    private AiInfoServiceImpl aiInfoService;

    private AiVendorRepository aiVendorRepository;

    private AiModelFamilyRepository aiModelFamilyRepository;

    private AiModelRepository aiModelRepository;

    @BeforeEach
    void setUp() {
        aiVendorRepository = mock(AiVendorRepository.class);
        aiModelFamilyRepository = mock(AiModelFamilyRepository.class);
        aiModelRepository = mock(AiModelRepository.class);
        aiInfoService = new AiInfoServiceImpl(
                aiVendorRepository,
                aiModelFamilyRepository,
                aiModelRepository,
                new AiModelMapper(),
                new ObjectMapper()
        );
    }

    @Test
    @DisplayName("새 벤더 계층을 읽으면 벤더, 패밀리, 모델을 각각 저장한다")
    void run_createsVendorHierarchy() throws IOException {
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

        when(aiVendorRepository.findByName("OpenAI")).thenReturn(java.util.Optional.empty());
        when(aiVendorRepository.save(any(AiVendor.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiModelRepository.findByApiId("gpt-4.1")).thenReturn(java.util.Optional.empty());
        when(aiModelRepository.save(any(AiModel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        aiInfoService.run();

        ArgumentCaptor<AiVendor> vendorCaptor = ArgumentCaptor.forClass(AiVendor.class);
        verify(aiVendorRepository).save(vendorCaptor.capture());
        AiVendor savedVendor = vendorCaptor.getValue();
        assertThat(savedVendor.getName()).isEqualTo("OpenAI");
        assertThat(savedVendor.getOfficialUrl()).isEqualTo("https://openai.com");
        assertThat(savedVendor.getIsActive()).isTrue();
        assertThat(savedVendor.getIsDeprecated()).isFalse();

        AiModelFamily savedFamily = savedVendor.getModelFamilies().getFirst();
        assertThat(savedFamily.getVendor()).isSameAs(savedVendor);
        assertThat(savedFamily.getFamilyName()).isEqualTo("GPT-4.1");
        assertThat(savedFamily.getCommonDescription()).isEqualTo("Flagship family");

        ArgumentCaptor<AiModel> modelCaptor = ArgumentCaptor.forClass(AiModel.class);
        verify(aiModelRepository).save(modelCaptor.capture());
        AiModel savedModel = modelCaptor.getValue();
        assertThat(savedModel.getFamily()).isSameAs(savedFamily);
        assertThat(savedModel.getApiId()).isEqualTo("gpt-4.1");
        assertThat(savedModel.getReleaseDate()).isEqualTo(LocalDate.of(2025, 1, 10));
        assertThat(savedModel.getInputModalities()).containsExactly("text", "image");
        assertThat(savedModel.getOutputModalities()).containsExactly("text");
    }

    @Test
    @DisplayName("이미 존재하는 벤더 계층은 중복 생성하지 않고 값을 갱신한다")
    void run_updatesExistingVendorHierarchy() throws IOException {
        AiVendor vendor = AiVendor.builder()
                .name("OpenAI")
                .officialUrl("https://old.example.com")
                .isActive(false)
                .isDeprecated(true)
                .build();
        AiModelFamily family = AiModelFamily.builder()
                .vendor(vendor)
                .familyName("GPT-4.1")
                .commonDescription("old family")
                .build();
        vendor.getModelFamilies().add(family);

        AiModel model = AiModel.builder()
                .family(family)
                .modelName("old-model")
                .apiId("gpt-4.1")
                .contextWindow(32000)
                .maxOutputTokens(1024)
                .releaseDate(LocalDate.of(2024, 1, 1))
                .isPreview(true)
                .modelImageUrl("https://old.example.com/model.png")
                .build();
        family.getModels().add(model);

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
                        "common_description": "Updated family",
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

        when(aiVendorRepository.findByName("OpenAI")).thenReturn(java.util.Optional.of(vendor));
        when(aiModelRepository.findByApiId("gpt-4.1")).thenReturn(java.util.Optional.of(model));

        aiInfoService.run();

        assertThat(vendor.getOfficialUrl()).isEqualTo("https://openai.com");
        assertThat(vendor.getIsActive()).isTrue();
        assertThat(vendor.getIsDeprecated()).isFalse();
        assertThat(family.getCommonDescription()).isEqualTo("Updated family");
        assertThat(model.getModelName()).isEqualTo("GPT-4.1");
        assertThat(model.getContextWindow()).isEqualTo(128000);
        assertThat(model.getMaxOutputTokens()).isEqualTo(4096);
        assertThat(model.getReleaseDate()).isEqualTo(LocalDate.of(2025, 1, 10));
        assertThat(model.getIsPreview()).isFalse();
        assertThat(model.getInputModalities()).containsExactly("text", "image");
        assertThat(model.getOutputModalities()).containsExactly("text");

        verify(aiVendorRepository, never()).save(any(AiVendor.class));
        verify(aiModelFamilyRepository, never()).save(any(AiModelFamily.class));
        verify(aiModelRepository, never()).save(any(AiModel.class));
    }

    @Test
    @DisplayName("JSON 파일을 읽지 못하면 저장 로직을 수행하지 않는다")
    void run_whenJsonFileMissing_doesNothing() {
        ReflectionTestUtils.setField(
                aiInfoService,
                "jsonFilePath",
                tempDir.resolve("missing-ai-info.json").toString()
        );

        aiInfoService.run();

        verifyNoInteractions(aiVendorRepository, aiModelFamilyRepository, aiModelRepository);
    }

    private Path writeJson(String json) throws IOException {
        Path jsonFile = tempDir.resolve("ai-info.json");
        Files.writeString(jsonFile, json);
        return jsonFile;
    }
}
