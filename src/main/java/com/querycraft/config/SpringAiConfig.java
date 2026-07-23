package com.querycraft.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class SpringAiConfig {

    private static final Logger log = LoggerFactory.getLogger(SpringAiConfig.class);

    @Value("${querycraft.vectorstore.storage-path:./data/vector_store.json}")
    private String vectorStorePath;

    /**
     * In-Memory Chat Memory for session-bound multi-turn conversations.
     */
    @Bean
    public ChatMemory chatMemory() {
        return new InMemoryChatMemory();
    }

    /**
     * Text splitter configuration for chunking ingested schema files.
     */
    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter(600, 400, 10, 5000, true);
    }

    /**
     * Fallback lightweight embedding model if no cloud provider key is present.
     */
    @Bean
    @ConditionalOnMissingBean(EmbeddingModel.class)
    public EmbeddingModel fallbackEmbeddingModel() {
        log.info("Configuring Fallback Embedding Model for local execution.");
        return new EmbeddingModel() {
            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                List<Embedding> embeddings = new ArrayList<>();
                int idx = 0;
                for (String text : request.getInstructions()) {
                    List<Double> vector = createVector(text);
                    embeddings.add(new Embedding(vector, idx++));
                }
                return new EmbeddingResponse(embeddings);
            }

            @Override
            public List<Double> embed(String text) {
                return createVector(text);
            }

            @Override
            public List<Double> embed(Document document) {
                return embed(document.getContent());
            }

            private List<Double> createVector(String text) {
                List<Double> vector = new ArrayList<>(1536);
                int hash = text != null ? text.hashCode() : 0;
                for (int i = 0; i < 1536; i++) {
                    vector.add(Math.sin(hash + i) / 10.0);
                }
                return vector;
            }
        };
    }

    /**
     * SimpleVectorStore bean with JSON file persistence.
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore store = new SimpleVectorStore(embeddingModel);
        File storeFile = new File(vectorStorePath);
        if (storeFile.exists()) {
            try {
                store.load(storeFile);
                log.info("Loaded persisted vector store from: {}", storeFile.getAbsolutePath());
            } catch (Exception e) {
                log.warn("Could not load existing vector store file. Starting fresh. Error: {}", e.getMessage());
            }
        }
        return store;
    }
}
