package com.Sharan.job_search_agent.service;

import com.Sharan.job_search_agent.model.UserProfile;
import com.Sharan.job_search_agent.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    @Transactional
    public UserProfile createOrUpdateProfile(UserProfile incoming) {

        Optional<UserProfile> existingOpt =
                userProfileRepository.findByUserId(incoming.getUserId());

        if (existingOpt.isEmpty()) {
            log.info("Creating new profile for userId: {}", incoming.getUserId());
            return userProfileRepository.save(incoming);
        }


        UserProfile existing = existingOpt.get();

        if (incoming.getName() != null) {
            existing.setName(incoming.getName());
        }
        if (incoming.getEmail() != null) {
            existing.setEmail(incoming.getEmail());
        }
        if (incoming.getSkills() != null) {
            existing.setSkills(incoming.getSkills());
        }
        if (incoming.getExperienceYears() != null) {
            existing.setExperienceYears(incoming.getExperienceYears());
        }
        if (incoming.getCurrentRole() != null) {
            existing.setCurrentRole(incoming.getCurrentRole());
        }
        if (incoming.getPreferredRoles() != null) {
            existing.setPreferredRoles(incoming.getPreferredRoles());
        }
        if (incoming.getPreferredLocations() != null) {
            existing.setPreferredLocations(incoming.getPreferredLocations());
        }
        if (incoming.getPreferredRemote() != null) {
            existing.setPreferredRemote(incoming.getPreferredRemote());
        }
        if (incoming.getSalaryMin() != null) {
            existing.setSalaryMin(incoming.getSalaryMin());
        }
        if (incoming.getSalaryMax() != null) {
            existing.setSalaryMax(incoming.getSalaryMax());
        }


        log.info("Updated existing profile for userId: {}", incoming.getUserId());
        return userProfileRepository.save(existing);
    }

    public Optional<UserProfile> getProfile(String userId) {
        return userProfileRepository.findByUserId(userId);
    }

    public boolean profileExists(String userId) {
        return userProfileRepository.existsByUserId(userId);
    }
}