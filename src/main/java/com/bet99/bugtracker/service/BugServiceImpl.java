package com.bet99.bugtracker.service;

import com.bet99.bugtracker.dto.BugResponse;
import com.bet99.bugtracker.dto.CreateBugRequest;
import com.bet99.bugtracker.model.Bug;
import com.bet99.bugtracker.model.Severity;
import com.bet99.bugtracker.repository.BugRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BugServiceImpl implements BugService {

    private final BugRepository bugRepository;

    public BugServiceImpl(BugRepository bugRepository) {
        this.bugRepository = bugRepository;
    }

    @Override
    @Transactional
    public BugResponse create(CreateBugRequest request) {
        Bug bug = new Bug();
        bug.setBugTitle(request.getBugTitle());
        bug.setDescription(request.getDescription());
        bug.setSeverity(request.getSeverity());
        bug.setStatus(request.getStatus());

        Bug saved = bugRepository.save(bug);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BugResponse> list(Optional<Severity> severity) {
        List<Bug> bugs = severity
                .map(bugRepository::findBySeverity)
                .orElseGet(bugRepository::findAll);

        return bugs.stream().map(this::toResponse).collect(Collectors.toList());
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

