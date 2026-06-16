package com.Sharan.job_search_agent.agent;

import com.Sharan.job_search_agent.model.AgentMemory;
import com.Sharan.job_search_agent.repository.AgentMemoryRepository;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostgresChatMemoryStore implements ChatMemoryStore {

    private final AgentMemoryRepository agentMemoryRepository;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {

        String id = memoryId.toString();
        log.debug("Loading memory for: {}", id);

        return agentMemoryRepository.findByMemoryId(id)
                .map(memory -> {
                    try {
                        return ChatMessageDeserializer.messagesFromJson(
                                memory.getMessages()
                        );
                    } catch (Exception e) {
                        log.error(
                                "Failed to deserialize memory for {}: {}",
                                id,
                                e.getMessage(),
                                e
                        );
                        return Collections.<ChatMessage>emptyList();
                    }
                })
                .orElse(Collections.emptyList());
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {

        String id = memoryId.toString();

        log.debug("Saving memory for: {} | {} messages", id, messages.size());

        try {

            String json = ChatMessageSerializer.messagesToJson(messages);

            AgentMemory memory = agentMemoryRepository.findByMemoryId(id).orElse(AgentMemory.builder()
                                            .memoryId(id)
                                            .build());
            memory.setMessages(json);
            agentMemoryRepository.save(memory);
        } catch (Exception e) {
            log.error(
                    "Failed to serialize memory for {}: {}",
                    id,
                    e.getMessage(),
                    e
            );
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {

        String id = memoryId.toString();

        log.info("Clearing memory for: {}", id);

        agentMemoryRepository.findByMemoryId(id)
                .ifPresent(agentMemoryRepository::delete);
    }
}