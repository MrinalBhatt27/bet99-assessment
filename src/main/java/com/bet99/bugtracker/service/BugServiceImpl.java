package com.bet99.bugtracker.service;

import com.bet99.bugtracker.dto.BugResponse;
import com.bet99.bugtracker.dto.CreateBugRequest;
import com.bet99.bugtracker.exception.BugNotFoundException;
import com.bet99.bugtracker.model.Bug;
import com.bet99.bugtracker.model.BugStatus;
import com.bet99.bugtracker.model.Severity;
import com.bet99.bugtracker.repository.BugRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BugServiceImpl implements BugService {

    private static final Logger log = LoggerFactory.getLogger(BugServiceImpl.class);

    private final BugRepository bugRepository;

    public BugServiceImpl(BugRepository bugRepository) {
        this.bugRepository = bugRepository;
    }

    @Override
    @Transactional
    public BugResponse create(CreateBugRequest request) {
        log.info("Creating bug: title='{}', severity={}, status={}",
                request.getBugTitle(), request.getSeverity(), request.getStatus());

        Bug bug = new Bug();
        bug.setBugTitle(request.getBugTitle());
        bug.setDescription(request.getDescription());
        bug.setSeverity(request.getSeverity());
        bug.setStatus(request.getStatus());

        Bug saved = bugRepository.save(bug);
        log.info("Bug created with id={}", saved.getId());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BugResponse> list(Optional<Severity> severity, Optional<BugStatus> status) {
        log.debug("Listing bugs: severity={}, status={}", severity.orElse(null), status.orElse(null));

        List<Bug> bugs;
        if (severity.isPresent() && status.isPresent()) {
            bugs = bugRepository.findBySeverityAndStatus(severity.get(), status.get());
        } else if (severity.isPresent()) {
            bugs = bugRepository.findBySeverity(severity.get());
        } else if (status.isPresent()) {
            bugs = bugRepository.findByStatus(status.get());
        } else {
            bugs = bugRepository.findAll();
        }

        return bugs.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BugResponse updateStatus(Long id, BugStatus status) {
        Optional<Bug> found = bugRepository.findById(id);
        if (!found.isPresent()) {
            log.warn("Bug not found for status update: id={}", id);
            throw new BugNotFoundException(id);
        }
        Bug bug = found.get();
        log.info("Updating bug id={} status: {} -> {}", id, bug.getStatus(), status);
        bug.setStatus(status);
        // Hibernate dirty-check flushes the change automatically within this transaction
        return toResponse(bug);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!bugRepository.findById(id).isPresent()) {
            log.warn("Bug not found for deletion: id={}", id);
            throw new BugNotFoundException(id);
        }
        log.info("Deleting bug id={}", id);
        bugRepository.deleteById(id);
    }

    private BugResponse toResponse(Bug bug) {
        BugResponse response = new BugResponse();
        response.setId(bug.getId());
        response.setBugTitle(bug.getBugTitle());
        response.setDescription(bug.getDescription());
        response.setSeverity(bug.getSeverity());
        response.setStatus(bug.getStatus());
        response.setCreatedAt(DateTimeFormatter.ISO_INSTANT.format(bug.getCreatedAt()));
        return response;
    }
}
