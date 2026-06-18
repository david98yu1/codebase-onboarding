package com.onboarding.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.List;
import java.util.Map;

@Service
public class BedrockService {

    private final BedrockRuntimeClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${aws.bedrock.modelId}")
    private String modelId;

    public BedrockService(
        @Value("${aws.accessKey}") String accessKey,
        @Value("${aws.secretKey}") String secretKey,
        @Value("${aws.region}") String region
    ) {
        this.client = BedrockRuntimeClient.builder()
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
            ))
            .build();
    }

    public String invoke(String prompt) {
        try {
            Map<String, Object> body = Map.of(
                "anthropic_version", "bedrock-2023-05-31",
                "max_tokens", 1024,
                "messages", List.of(
                    Map.of("role", "user", "content", prompt)
                )
            );

            String bodyJson = objectMapper.writeValueAsString(body);

            InvokeModelRequest request = InvokeModelRequest.builder()
                .modelId(modelId)
                .contentType("application/json")
                .accept("application/json")
                .body(SdkBytes.fromUtf8String(bodyJson))
                .build();

            InvokeModelResponse response = client.invokeModel(request);
            String responseBody = response.body().asUtf8String();

            // Parse response: {"content": [{"text": "..."}]}
            Map<?, ?> parsed = objectMapper.readValue(responseBody, Map.class);
            List<?> content = (List<?>) parsed.get("content");
            Map<?, ?> first = (Map<?, ?>) content.get(0);
            return (String) first.get("text");

        } catch (Exception e) {
            throw new RuntimeException("Bedrock invocation failed: " + e.getMessage(), e);
        }
    }
}
