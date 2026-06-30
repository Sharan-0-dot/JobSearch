package com.Sharan.job_search_agent.listener;

import com.Sharan.job_search_agent.event.JobSavedEvent;
import com.Sharan.job_search_agent.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class JobSavedListener {

    private final EmbeddingService embeddingService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onJobSaved(JobSavedEvent event) {
        embeddingService.embedAndSaveJob(event.job());
    }
}