package com.Sharan.job_search_agent.agent;

import com.Sharan.job_search_agent.tools.JSearchTool;
import com.Sharan.job_search_agent.tools.JobRagTool;
import com.Sharan.job_search_agent.tools.SkillExtractorTool;
import com.Sharan.job_search_agent.tools.UserProfileTool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentConfig {

    @Value("${agent.memory.max-messages:20}")
    private int maxMessages;


    @Bean
    public JobSearchAgent jobSearchAgent(
            ChatLanguageModel chatLanguageModel,
            PostgresChatMemoryStore memoryStore,
            JSearchTool jSearchTool,
            JobRagTool jobRagTool,
            UserProfileTool userProfileTool,
            SkillExtractorTool skillExtractorTool) {

        return AiServices.builder(JobSearchAgent.class)

                .chatLanguageModel(chatLanguageModel)

                .tools(jSearchTool, jobRagTool, userProfileTool, skillExtractorTool)

                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(maxMessages)
                        .chatMemoryStore(memoryStore)
                        .build())
                .build();
    }
}