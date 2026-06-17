package com.Sharan.job_search_agent.controller;

import com.Sharan.job_search_agent.model.UserProfile;
import com.Sharan.job_search_agent.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserProfileService userProfileService;

    @PostMapping
    public ResponseEntity<?> createOrUpdateProfile(@RequestBody UserProfile profile) {

        if (profile.getUserId() == null || profile.getUserId().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "userId is required"));
        }

        boolean existedBefore = userProfileService.profileExists(profile.getUserId());

        UserProfile saved = userProfileService.createOrUpdateProfile(profile);

        return ResponseEntity.ok(Map.of(
                "message", existedBefore ? "Profile updated" : "Profile created",
                "profile", saved
        ));
    }

    // GET /api/profile/{userId}
    @GetMapping("/{userId}")
    public ResponseEntity<?> getProfile(@PathVariable String userId) {

        return userProfileService.getProfile(userId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound()
                        .build());
    }
}