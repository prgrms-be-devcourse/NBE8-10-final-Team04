package back.domain.info.service;

import back.domain.info.entity.ModelBenchmark;
import back.domain.info.mapper.ModelStatMapper;
import back.domain.info.repository.ModelBenchmarkRepository;
import back.global.storage.OciObjectStorageReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ModelBenchmarkServiceImplTest {

    private static final String BASE_PATH = "data/ai-info/model_benchmarks_records.json";

    private ModelBenchmarkRepository benchmarkRepository;
    private OciObjectStorageReader storageReader;
    private ModelBenchmarkServiceImpl service;

    @BeforeEach
    void setUp() {
        benchmarkRepository = mock(ModelBenchmarkRepository.class);
        storageReader = mock(OciObjectStorageReader.class);
        service = new ModelBenchmarkServiceImpl(
                new ObjectMapper(),
                storageReader,
                benchmarkRepository,
                new ModelStatMapper()
        );
    }

    @Test
    void run_savesNewBenchmarks() {
        String json = """
                [
                  {
                    "aa_slug": "gpt-4.1",
                    "metric_type": "CODING",
                    "metric_value": 92.30,
                    "measured_at": "2026-03-27 09:30:00",
                    "unit": "score"
                  },
                  {
                    "aa_slug": "claude-sonnet",
                    "metric_type": "PRICE_BLENDED",
                    "metric_value": 1.50,
                    "measured_at": "2026-03-27 10:00:00",
                    "unit": "usd"
                  }
                ]
                """;
        when(storageReader.readText(BASE_PATH)).thenReturn(json);
        when(benchmarkRepository.save(any(ModelBenchmark.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.run();

        ArgumentCaptor<ModelBenchmark> captor = ArgumentCaptor.forClass(ModelBenchmark.class);
        verify(benchmarkRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(ModelBenchmark::getModelApiId)
                .containsExactly("gpt-4.1", "claude-sonnet");
    }

    @Test
    void run_savesBenchmarkEvenWhenSameModelMetricExists() {
        String json = """
                [
                  {
                    "aa_slug": "gpt-4.1",
                    "metric_type": "CODING",
                    "metric_value": 92.30,
                    "measured_at": "2026-03-27 09:30:00",
                    "unit": "score"
                  }
                ]
                """;
        when(storageReader.readText(BASE_PATH)).thenReturn(json);
        when(benchmarkRepository.save(any(ModelBenchmark.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.run();

        ArgumentCaptor<ModelBenchmark> captor = ArgumentCaptor.forClass(ModelBenchmark.class);
        verify(benchmarkRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getModelApiId()).isEqualTo("gpt-4.1");
        assertThat(captor.getValue().getMetricValue()).isEqualByComparingTo("92.30");
        assertThat(captor.getValue().getUnit()).isEqualTo("score");
    }

    @Test
    void run_ignoresInvalidJson() {
        when(storageReader.readText(BASE_PATH)).thenReturn("{broken");

        service.run();

        verifyNoInteractions(benchmarkRepository);
    }
}
