package back.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
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
        var requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(3))   // 연결 수립 최대 3s
                .setResponseTimeout(Timeout.ofSeconds(10)) // 응답 대기 최대 10s
                .build();

        var httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();

        var factory = new HttpComponentsClientHttpRequestFactory(httpClient);

        return RestClient.builder()
                .baseUrl(embeddingBaseUrl)
                .requestFactory(factory)
                .build();
    }
}