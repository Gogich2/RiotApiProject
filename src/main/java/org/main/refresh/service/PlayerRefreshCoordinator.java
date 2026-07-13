package org.main.refresh.service;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.main.exception.NotFoundException;
import org.main.refresh.dto.PlayerRefreshStatusDto;
import org.main.refresh.entity.PlayerRefreshJobEntity;
import org.main.refresh.entity.RefreshSource;
import org.main.refresh.entity.RefreshState;
import org.main.refresh.repository.PlayerRefreshJobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class PlayerRefreshCoordinator {

    private static final List<RefreshState> ACTIVE_STATES = List.of(
            RefreshState.QUEUED,
            RefreshState.RUNNING
    );

    private final PlayerRefreshJobRepository jobRepository;

    private final PlayerRefreshWorker worker;

    private final TaskExecutor taskExecutor;

    private final Clock clock;

    private final Duration manualCooldown;

    public PlayerRefreshCoordinator(
            PlayerRefreshJobRepository jobRepository,
            PlayerRefreshWorker worker,
            TaskExecutor taskExecutor,
            Clock clock,
            @Value("${app.refresh.manual-cooldown:120s}") Duration manualCooldown
    ) {
        this.jobRepository = jobRepository;
        this.worker = worker;
        this.taskExecutor = taskExecutor;
        this.clock = clock;
        this.manualCooldown = manualCooldown;
    }

    @Transactional
    public PlayerRefreshStatusDto enqueue(String puuid, RefreshSource source) {
        Optional<PlayerRefreshJobEntity> active = jobRepository.
                findFirstByPuuidAndStateInOrderByRequestedAtDesc(puuid, ACTIVE_STATES);
        if (active.isPresent()) {
            return PlayerRefreshStatusDto.from(active.get());
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (source == RefreshSource.MANUAL) {
            enforceCooldown(puuid, now);
        }

        PlayerRefreshJobEntity job = new PlayerRefreshJobEntity();
        job.setId(UUID.randomUUID());
        job.setPuuid(puuid);
        job.setSource(source);
        job.setState(RefreshState.QUEUED);
        job.setRequestedAt(now);
        try {
            jobRepository.saveAndFlush(job);
        } catch (DataIntegrityViolationException exception) {
            return jobRepository.findFirstByPuuidAndStateInOrderByRequestedAtDesc(puuid, ACTIVE_STATES).
                    map(PlayerRefreshStatusDto::from).
                    orElseThrow(() -> exception);
        }
        scheduleAfterCommit(job.getId());
        return PlayerRefreshStatusDto.from(job);
    }

    @Transactional(readOnly = true)
    public PlayerRefreshStatusDto latest(String puuid) {
        return jobRepository.findFirstByPuuidOrderByRequestedAtDesc(puuid).
                map(PlayerRefreshStatusDto::from).
                orElseThrow(() -> new NotFoundException("No refresh status found"));
    }

    private void enforceCooldown(String puuid, OffsetDateTime now) {
        jobRepository.findFirstByPuuidAndSourceOrderByRequestedAtDesc(puuid, RefreshSource.MANUAL).
                ifPresent(previous -> {
                    OffsetDateTime allowedAt = previous.getRequestedAt().plus(manualCooldown);
                    if (allowedAt.isAfter(now)) {
                        throw new RefreshCooldownException(Duration.between(now, allowedAt));
                    }
                });
    }

    private void scheduleAfterCommit(UUID jobId) {
        Runnable schedule = () -> taskExecutor.execute(() -> worker.run(jobId));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    schedule.run();
                }
            });
        } else {
            schedule.run();
        }
    }

    public static class RefreshCooldownException extends RuntimeException {

        private final Duration retryAfter;

        public RefreshCooldownException(Duration retryAfter) {
            super("This profile was refreshed recently. Try again shortly.");
            this.retryAfter = retryAfter;
        }

        public Duration getRetryAfter() {
            return retryAfter;
        }
    }
}
