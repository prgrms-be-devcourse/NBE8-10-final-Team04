package back.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private static final String STORAGE_TYPE_LOCAL = "local";

    @Value("${app.storage.type:local}")
    private String storageType;

    @Value("${app.storage.local.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${app.embedding.base-url}")
    private String embeddingBaseUrl;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (!STORAGE_TYPE_LOCAL.equalsIgnoreCase(storageType)) {
            return;
        }

        // 업로드된 파일을 정적 리소스로 제공
        registry.addResourceHandler("/uploads/**").addResourceLocations("file:" + uploadDir + "/");
    }

    @Bean
    public RestClient embeddingRestClient() {
        // PoolingHttpClientConnectionManager 기본값:
        //   maxConnPerRoute = 5  → 100 VU 에서 95개가 연결 풀 대기 → Tomcat 스레드 95개 묶임
        //   maxConnTotal    = 25 → 전체 연결 한도
        //
        // 100 VU 기준으로 충분한 연결 수를 확보한다.
        // setConnectionRequestTimeout: 풀에서 연결을 빌리지 못하면 3s 후 즉시 실패
        //   → 풀이 꽉 찼을 때 Tomcat 스레드가 무한 대기하는 것을 방지
        var socketConfig = SocketConfig.custom()
                .setSoTimeout(Timeout.ofSeconds(10))
                .build();

        var connManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultSocketConfig(socketConfig)
                .setMaxConnPerRoute(200) // embed 서버 1개 엔드포인트로의 최대 동시 연결 수
                .setMaxConnTotal(200)    // 전체 최대 연결 수
                .build();

        var requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(3))
                .setConnectionRequestTimeout(Timeout.ofSeconds(3)) // 풀 연결 획득 대기 최대 3s
                .setResponseTimeout(Timeout.ofSeconds(10))
                .build();

        var httpClient = HttpClients.custom()
                .setConnectionManager(connManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        return RestClient.builder()
                .baseUrl(embeddingBaseUrl)
                .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
                .build();
    }
}