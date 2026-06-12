package com.Sharan.job_search_agent.repository;

import com.Sharan.job_search_agent.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByUserId(String userId);

    boolean existsByUserId(String userId);

    boolean existsByEmail(String email);

    Optional<UserProfile> findByEmail(String email);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE user_profiles
            SET resume_embedding = CAST(:embedding AS vector)
            WHERE user_id = :userId
            """, nativeQuery = true)
    void updateResumeEmbedding(
            @Param("userId") String userId,
            @Param("embedding") String embedding
    );

    @Modifying
    @Transactional
    @Query("""
            UPDATE UserProfile u
            SET u.extractedSkills = :skills
            WHERE u.userId = :userId
            """)
    void updateExtractedSkills(
            @Param("userId") String userId,
            @Param("skills") String[] skills
    );


    @Modifying
    @Transactional
    @Query("""
            UPDATE UserProfile u
            SET u.resumeText = :resumeText
            WHERE u.userId = :userId
            """)
    void updateResumeText(
            @Param("userId") String userId,
            @Param("resumeText") String resumeText
    );

    @Query("""
            SELECT u FROM UserProfile u
            WHERE u.preferredRemote = true
            """)
    java.util.List<UserProfile> findRemotePreferringUsers();
}
