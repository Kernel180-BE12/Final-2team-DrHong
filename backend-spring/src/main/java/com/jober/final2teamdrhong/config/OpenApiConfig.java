package com.jober.final2teamdrhong.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${app.environment.development:true}")
    private boolean isDevelopment;

    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("notimo API Documentation")
                .description("로컬 회원가입 및 이메일 인증 시스템을 포함한 notimo 백엔드 API 문서입니다.")
                .version("v1.0")
                .contact(new Contact()
                        .name("notimo Development Team")
                        .email("dev@notimo.com"));

        // 개발환경에 따른 서버 설정
        Server server = new Server();
        if (isDevelopment) {
            server.url("http://localhost:8080")
                  .description("로컬 개발 서버");
        } else {
            server.url("https://api.notimo.com")
                  .description("운영 서버");
        }

        return new OpenAPI()
                .info(info)
                .servers(List.of(server));
    }
}