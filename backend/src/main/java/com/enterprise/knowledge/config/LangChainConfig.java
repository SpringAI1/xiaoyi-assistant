package com.enterprise.knowledge.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChainConfig {

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.withMaxMessages(20);
    }

    @Bean
    public ChatLanguageModel qwenChatModel(
            @Value("${langchain.qwen.api-key}") String apiKey,
            @Value("${langchain.qwen.base-url}") String baseUrl,
            @Value("${langchain.qwen.model:qwen-turbo}") String model,
            @Value("${langchain.qwen.temperature:0.7}") Double temperature) {

        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(model)
                .temperature(temperature)
                .topP(1.0)
                .maxTokens(4096)
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel(
            @Value("${langchain.qwen.api-key}") String apiKey,
            @Value("${langchain.qwen.base-url}") String baseUrl) {

        return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName("text-embedding-v3")
                .dimensions(512)
                .build();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }
}