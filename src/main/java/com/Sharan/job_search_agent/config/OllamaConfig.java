package com.Sharan.job_search_agent.config;

import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Duration;

@Configuration
@Profile("local")
public class OllamaConfig {

    @Value("${langchain4j.ollama.chat-model.base-url}")
    private String ollamaBaseUrl;

    @Value("${langchain4j.ollama.chat-model.model-name}")
    private String chatModelName;

    @Value("${langchain4j.ollama.embedding-model.model-name}")
    private String embeddingModelName;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(chatModelName)
                .temperature(0.3)
                .timeout(Duration.ofSeconds(120))
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        return OllamaEmbeddingModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(embeddingModelName)
                .timeout(Duration.ofSeconds(60))
                .build();
    }
}
