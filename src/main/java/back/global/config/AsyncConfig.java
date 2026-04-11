package back.global.config;

import java.util.concurrent.Executor;

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

    // SkillSearchServiceImpl 의 CompletableFuture 병렬 임베딩 전용 풀
    //
    // 크기 근거:
    //   QUERY_LIMIT = 7 → 요청 1건당 최대 7개 embed 호출을 동시에 실행
    //   스레드 수를 7 초과로 늘려도 임베딩 서비스 자체가 병목이므로 효과 없음
    //   ForkJoinPool.commonPool() 미사용 이유: 공용 풀을 점유하면 GC·기타 비동기 작업에 영향
    @Bean(name = "skillSearchExecutor")
    public Executor skillSearchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(7);
        executor.setMaxPoolSize(7);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("skill-search-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
