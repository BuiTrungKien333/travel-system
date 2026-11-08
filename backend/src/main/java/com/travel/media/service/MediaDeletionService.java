package com.travel.media.service;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.media.repository.MediaDeletionRepository;
import com.travel.media.repository.MediaDeletionRepository.Deletion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class MediaDeletionService {
    private final MediaDeletionRepository repository;
    private final CloudinaryImageDeletion cloud;
    private final PlatformTransactionManager transactions;
    private final Clock clock;

    public void delete(UUID id, UUID owner) {
        Deletion job = new TransactionTemplate(transactions).execute(status -> {
            var existing = repository.find(id);
            if (existing.isPresent()) {
                if (!existing.get().userId().equals(owner)) throw new AppException(ErrorCode.MEDIA_NOT_FOUND);
                return existing.get();
            }
            var current = repository.lockOwnedPhoto(id, owner);
            // Another request may finish between the first receipt read and the aggregate lock.
            if (current.isEmpty()) return repository.find(id).filter(receipt -> receipt.userId().equals(owner))
                    .orElseThrow(() -> new AppException(ErrorCode.MEDIA_NOT_FOUND));
            var photo = current.get();
            repository.reserve(photo, clock.instant());
            return repository.find(id).orElseThrow();
        });
        if (Objects.requireNonNull(job).completedAt() == null) process(job);
    }

    private void process(Deletion job) {
        try {
            cloud.destroy(job.publicId());
            new TransactionTemplate(transactions).executeWithoutResult(status -> repository.finish(job, clock.instant()));
        } catch (RuntimeException failure) {
            repository.retryLater(job.mediaId(), clock.instant());
            throw failure;
        }
    }

    public void retryPending() {
        repository.reserveArchived(clock.instant());
        for (UUID id : repository.due(clock.instant())) {
            try {
                var job = repository.find(id);
                if (job.isPresent() && job.get().completedAt() == null) process(job.get());
            } catch (RuntimeException failure) {
                log.warn("Media cleanup deferred ({})", failure.getClass().getSimpleName());
            }
        }
    }

    /** An upload started before deletion may finish later; never attach that file back. */
    public void cleanLateUpload(UUID owner, UUID memory, String hash, String publicId) {
        var receipt = new TransactionTemplate(transactions).execute(status -> repository.removedUpload(memory, hash)
                .or(() -> repository.reserveArchivedUpload(owner, memory, hash, publicId, clock.instant())));
        Objects.requireNonNull(receipt).ifPresent(id -> {
            repository.reopen(id, clock.instant());
            try {
                process(repository.find(id).orElseThrow());
            } catch (RuntimeException failure) {
                log.warn("Late upload cleanup deferred ({})", failure.getClass().getSimpleName());
            }
        });
    }
}
