package com.Sharan.job_search_agent.repository;

import com.Sharan.job_search_agent.model.AgentMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgentMemoryRepository extends JpaRepository<AgentMemory, UUID> {


    Optional<AgentMemory> findByMemoryId(String memoryId);

    boolean existsByMemoryId(String memoryId);

    @Modifying
    @Transactional
    void deleteByMemoryId(String memoryId);

    // Update messages for existing memory
    // Called after every agent interaction
    @Modifying
    @Transactional
    @Query("""
            UPDATE AgentMemory a
            SET a.messages = :messages
            WHERE a.memoryId = :memoryId
            """)
    void updateMessages(
            @Param("memoryId") String memoryId,
            @Param("messages") String messages
    );
}
