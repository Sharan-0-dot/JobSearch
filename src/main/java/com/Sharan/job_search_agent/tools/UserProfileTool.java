package com.Sharan.job_search_agent.tools;

import com.Sharan.job_search_agent.model.UserProfile;
import com.Sharan.job_search_agent.repository.UserProfileRepository;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class UserProfileTool {

    private final UserProfileRepository userProfileRepository;


    @Tool("""
    Retrieve the user's saved profile with skills, experience, and preferences.
    
    Call this tool:
    - BEFORE ranking or personalizing jobs
    - When user asks about their profile
    - Before giving career advice
    
    Returns:
    - User skills and experience level
    - Preferred locations
    - Current role and background
    - Resume information
    
    Use the returned profile data to personalize job recommendations.
    """)
    public String getUserProfile(@P("The user's unique ID") String userId) {
        log.info("UserProfileTool invoked | userId: {}", userId);

        return userProfileRepository.findByUserId(userId)
                .map(this::buildProfileSummary)
                .orElse("No profile found for user: " + userId +
                        ". Ask the user to upload their resume first.");
    }


    private String buildProfileSummary(UserProfile profile) {
        StringBuilder sb = new StringBuilder();
        sb.append("User Profile for: ").append(profile.getUserId()).append("\n");

        if (profile.getName() != null)
            sb.append("Name: ").append(profile.getName()).append("\n");

        if (profile.getExperienceYears() != null)
            sb.append("Experience: ").append(profile.getExperienceYears()).append(" years\n");

        if (profile.getCurrentRole() != null)
            sb.append("Current Role: ").append(profile.getCurrentRole()).append("\n");

        if (profile.getSkills() != null && profile.getSkills().length > 0)
            sb.append("Skills: ").append(String.join(", ", profile.getSkills())).append("\n");

        if (profile.getExtractedSkills() != null && profile.getExtractedSkills().length > 0)
            sb.append("Resume Skills: ")
                    .append(String.join(", ", profile.getExtractedSkills())).append("\n");

        if (profile.getPreferredLocations() != null && profile.getPreferredLocations().length > 0)
            sb.append("Preferred Locations: ")
                    .append(String.join(", ", profile.getPreferredLocations())).append("\n");

        if (Boolean.TRUE.equals(profile.getPreferredRemote()))
            sb.append("Open to Remote: Yes\n");

        return sb.toString();
    }
}