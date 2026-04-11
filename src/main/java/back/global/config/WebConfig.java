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
        // HC5에서 timeout 은 두 레이어로 나뉜다:
        //   setResponseTimeout → response headers 수신까지의 timeout (HC4의 waitForContinue)
        //   SocketConfig.setSoTimeout → socket read 마다 적용되는 timeout (HC4의 socketTimeout)
        //
        // body 전송 지연(embed 서버가 헤더는 빠르게, body는 28s 후 전송)을 막으려면
        // setSoTimeout 이 반드시 필요하다. setResponseTimeout 만으로는 부족하다.
        var socketConfig = SocketConfig.custom()
                .setSoTimeout(Timeout.ofSeconds(10)) // 소켓 read 블로킹 최대 10s
                .build();

        var connManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultSocketConfig(socketConfig)
                .build();

        var requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(3))   // TCP 연결 수립 최대 3s
                .setResponseTimeout(Timeout.ofSeconds(10)) // response headers 수신 최대 10s
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