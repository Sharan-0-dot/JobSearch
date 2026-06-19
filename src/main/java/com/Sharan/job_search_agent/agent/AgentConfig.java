package com.Sharan.job_search_agent.agent;

import com.Sharan.job_search_agent.tools.JSearchTool;
import com.Sharan.job_search_agent.tools.JobRagTool;
import com.Sharan.job_search_agent.tools.SkillExtractorTool;
import com.Sharan.job_search_agent.tools.UserProfileTool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
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
            SkillExtractorTool skillExtractorTool) throws Exception {

        log.info("Initializing JobSearchAgent...");

        Object rawJSearchTool = unwrapProxy(jSearchTool);
        Object rawJobRagTool = unwrapProxy(jobRagTool);
        Object rawUserProfileTool = unwrapProxy(userProfileTool);
        Object rawSkillExtractorTool = unwrapProxy(skillExtractorTool);

        return AiServices.builder(JobSearchAgent.class)
                .chatLanguageModel(chatLanguageModel)
                .tools(rawJSearchTool, rawJobRagTool, rawUserProfileTool, rawSkillExtractorTool)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(maxMessages)
                        .chatMemoryStore(memoryStore)
                        .build())
                .build();
    }

    private Object unwrapProxy(Object bean) throws Exception {
        if (AopUtils.isAopProxy(bean) && bean instanceof Advised advised) {
            return advised.getTargetSource().getTarget();
        }
        return bean;
    }
}