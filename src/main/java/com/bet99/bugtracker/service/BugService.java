package com.bet99.bugtracker.service;

import com.bet99.bugtracker.dto.BugResponse;
import com.bet99.bugtracker.dto.CreateBugRequest;
import com.bet99.bugtracker.model.Severity;

import java.util.List;
import java.util.Optional;

public interface BugService {
    BugResponse create(CreateBugRequest request);
    List<BugResponse> list(Optional<Severity> severity);
}

