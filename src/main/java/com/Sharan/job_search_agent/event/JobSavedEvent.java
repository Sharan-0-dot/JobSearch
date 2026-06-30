package com.Sharan.job_search_agent.event;

import com.Sharan.job_search_agent.model.JobListing;

public record JobSavedEvent(JobListing job) {}