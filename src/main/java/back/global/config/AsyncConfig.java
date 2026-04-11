package back.global.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("StartAiHubAsync-");
        executor.setWaitForTasksToCompleteOnShutdown(true);

        executor.initialize();
        return executor;
    }

    // SkillSearchServiceImpl 의 CompletableFuture 병렬 임베딩 전용 Executor
    //
    // Virtual Thread 를 사용하는 이유:
    //   embed() = 외부 HTTP 호출 = Blocking I/O
    //   플랫폼 스레드 풀(고정 크기)로 Blocking I/O 를 처리하면:
    //     → 스레드가 I/O 대기 중 OS 스레드를 점유
    //     → 100 VU × 7 태스크 = 700개 동시 요청 시 풀이 즉시 고갈 → TaskRejectedException
    //     → queueCapacity 를 늘려도 처리 스레드가 부족해 큐가 결국 꽉 참
    //
    //   Virtual Thread (Java 21) 는 I/O 대기 중 OS 스레드를 반환(mount/unmount)
    //     → 수천 개가 동시에 대기해도 OS 스레드 소비 없음
    //     → 큐 없이 태스크마다 즉시 가상 스레드 생성 → TaskRejectedException 원천 차단
    //     → Blocking I/O 병렬화에 가장 적합한 방식
    @Bean(name = "skillSearchExecutor")
    public Executor skillSearchExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
