package com.finance.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SUCCESS_EXAMPLE = "{\n  \"code\": 200,\n  \"message\": \"success\",\n  \"data\": {}\n}";
    private static final String BAD_REQUEST_EXAMPLE = "{\n  \"code\": 400,\n  \"message\": \"Invalid request parameters\",\n  \"data\": null\n}";
    private static final String NOT_FOUND_EXAMPLE = "{\n  \"code\": 404,\n  \"message\": \"Resource not found\",\n  \"data\": null\n}";
    private static final String SERVER_ERROR_EXAMPLE = "{\n  \"code\": 500,\n  \"message\": \"Internal server error\",\n  \"data\": null\n}";

    @Bean
    public OpenAPI financeOpenAPI() {
        return new OpenAPI()
                .components(new Components())
                .info(new Info()
                        .title("个人收支记账 APP API")
                        .version("v1")
                        .description("提供交易、预算、统计、通知等核心功能的后端接口文档")
                        .contact(new Contact().name("Finance Backend Team").email("support@example.com"))
                        .license(new License().name("Apache 2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8081").description("本地开发环境"))) ;
    }

    @Bean
    public OpenApiCustomizer globalResponseCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
                ApiResponses responses = operation.getResponses();
                addIfMissing(responses, "200", buildResponse("请求成功", SUCCESS_EXAMPLE));
                addIfMissing(responses, "400", buildResponse("参数错误", BAD_REQUEST_EXAMPLE));
                addIfMissing(responses, "404", buildResponse("资源不存在", NOT_FOUND_EXAMPLE));
                addIfMissing(responses, "500", buildResponse("服务器内部错误", SERVER_ERROR_EXAMPLE));
            }));
        };
    }

    private void addIfMissing(ApiResponses responses, String code, ApiResponse response) {
        if (!responses.containsKey(code)) {
            responses.addApiResponse(code, response);
        }
    }

    private ApiResponse buildResponse(String description, String exampleJson) {
        Schema<Object> schema = new Schema<>().$ref("#/components/schemas/ApiResponse");
        MediaType mediaType = new MediaType().schema(schema).example(exampleJson);
        Content content = new Content().addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE, mediaType);
        return new ApiResponse().description(description).content(content);
    }
}
