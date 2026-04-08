package back.global.swagger;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Start AI Hub API",
                version = "v1",
                description = "Auth, MCP, CommunityPost, Comment API 문서"))
@SecuritySchemes({
    @SecurityScheme(
            name = "bearerAuth",
            type = SecuritySchemeType.HTTP,
            scheme = "bearer",
            bearerFormat = "JWT",
            description = "사용자 Access Token 인증 (Authorization: Bearer {accessToken})"),
    @SecurityScheme(
            name = "mcpBearerAuth",
            type = SecuritySchemeType.HTTP,
            scheme = "bearer",
            bearerFormat = "MCP",
            description = "MCP 토큰 인증 (Authorization: Bearer mcp_xxx)")
})
public class OpenApiConfig {
}
