package back.domain.aimodel.service;

import back.domain.aimodel.config.AiModelProperties;
import back.domain.aimodel.config.GeminiProperties;
import back.domain.aimodel.dto.integrated.IntegratedVendor;
import back.domain.aimodel.dto.openrouter.OrModelsResponse;
import back.domain.aimodel.dto.openrouter.OrModelsResponse.OrModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.Models;
import com.google.genai.types.GenerateContentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelMergeServiceTest {

    @Mock AiModelProperties props;
    @Mock GeminiProperties  geminiProperties;
    @Mock ObjectMapper      objectMapper;
    @Mock Client            geminiClient;
    @Mock Models            geminiModels;

    ModelMergeServiceImpl mergeService;

    @BeforeEach
    void setUp() throws Exception {
        mergeService = new ModelMergeServiceImpl(props, geminiProperties, objectMapper);
        
        // geminiClient 직접 주입 (PostConstruct 우회)
        Field field = ModelMergeServiceImpl.class.getDeclaredField("geminiClient");
        field.setAccessible(true);
        field.set(mergeService, geminiClient);

        // Client의 models 필드에 mock Models 객체 주입
        Field modelsField = Client.class.getDeclaredField("models");
        modelsField.setAccessible(true);
        modelsField.set(geminiClient, geminiModels);

        // 불필요한 스터빙 경고를 막기 위해 lenient() 사용
        lenient().when(props.targetVendors()).thenReturn(List.of("openai", "anthropic", "google"));
        lenient().when(geminiProperties.model()).thenReturn("gemini-test-model");
    }

    // ── extractFamilyName 규칙 검증 ───────────────────────────────────────────

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "claude-opus-4,            CLAUDE-OPUS-4",
            "claude-3.7-sonnet,        CLAUDE-SONNET",
            "gpt-4o-mini,              GPT-4O",
            "gpt-4o-2024-11-20,        GPT-4O",
            "gpt-4-turbo,              GPT-4",
            "gpt-4-1106-preview,       GPT-4",
            "gpt-3.5-turbo-16k,        GPT",
            "o3-deep-research,         O3",
            "gemini-2.5-flash,         GEMINI",
            "gemini-2.5-09-2025,       GEMINI",
            "gemma-3-27b-it,           GEMMA-3-IT",
            "o1-mini,                  O1",
    })
    @DisplayName("1차 규칙 기반 패밀리명 추출")
    void extractFamilyName_rulesApplied(String modelName, String expected) throws Exception {
        Method method = ModelMergeServiceImpl.class.getDeclaredMethod("extractFamilyName", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(mergeService, modelName);
        assertThat(result).isEqualTo(expected.trim());
    }

    @Test
    @DisplayName("빅3 벤더만 포함되고 나머지는 제외된다")
    void merge_onlyTargetVendorsIncluded() {
        OrModelsResponse orResponse = new OrModelsResponse(List.of(
                orModel("anthropic/claude-opus-4"),
                orModel("openai/gpt-4o"),
                orModel("meta-llama/llama-3-8b"),   // 제외 대상
                orModel("mistralai/mistral-7b")      // 제외 대상
        ));

        List<IntegratedVendor> result = mergeService.merge(orResponse);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(IntegratedVendor::name)
                .containsExactlyInAnyOrder("Anthropic", "OpenAI");
    }

    @Test
    @DisplayName("같은 패밀리의 여러 모델은 하나의 패밀리로 그룹핑된다")
    void merge_sameFamily_groupedIntoOne() {
        OrModelsResponse orResponse = new OrModelsResponse(List.of(
                orModel("openai/gpt-4o"),
                orModel("openai/gpt-4o-mini"),
                orModel("openai/gpt-4o-2024-11-20"),
                orModel("openai/gpt-4o-2024-08-06")
        ));

        List<IntegratedVendor> result = mergeService.merge(orResponse);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).families()).hasSize(1);
        assertThat(result.get(0).families().get(0).familyName()).isEqualTo("GPT-4O");
    }

    @Test
    @DisplayName("미분류 모델이 없으면 Gemini를 호출하지 않는다")
    void merge_noUnresolved_geminiNotCalled() {
        OrModelsResponse orResponse = new OrModelsResponse(List.of(
                orModel("anthropic/claude-opus-4"),
                orModel("openai/gpt-4o")
        ));

        mergeService.merge(orResponse);

        verifyNoInteractions(geminiModels);
    }

    @Test
    @DisplayName("Gemini 판별 실패 시 미분류 모델은 제외하고 파이프라인 계속 진행")
    void merge_geminiFails_continuesWithoutUnresolved() throws Exception {
        // OTHERS를 유발하는 모델 ID (모든 토큰이 필터링되는 케이스)
        OrModelsResponse orResponse = new OrModelsResponse(List.of(
                orModel("openai/gpt-4o"),
                orModel("openai/mini-nano-lite")  // 전부 TIER_KEYWORDS → OTHERS
        ));

        when(geminiModels.generateContent(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Gemini 호출 실패"));

        assertThatCode(() -> mergeService.merge(orResponse)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("OR id에 슬래시가 없으면 스킵된다")
    void merge_invalidIdFormat_skipped() {
        OrModelsResponse orResponse = new OrModelsResponse(List.of(
                orModel("invalid-no-slash"),
                orModel("openai/gpt-4o")
        ));

        List<IntegratedVendor> result = mergeService.merge(orResponse);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("OpenAI");
    }

    @Test
    @DisplayName("벤더 표시명이 올바르게 정규화된다")
    void merge_vendorDisplayName_normalized() {
        OrModelsResponse orResponse = new OrModelsResponse(List.of(
                orModel("openai/gpt-4o"),
                orModel("anthropic/claude-opus-4"),
                orModel("google/gemini-2.5-pro")
        ));

        List<IntegratedVendor> result = mergeService.merge(orResponse);

        assertThat(result).extracting(IntegratedVendor::name)
                .containsExactlyInAnyOrder("OpenAI", "Anthropic", "Google");
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    private OrModel orModel(String id) {
        return new OrModel(id, id, null, null, null, null, null);
    }
}