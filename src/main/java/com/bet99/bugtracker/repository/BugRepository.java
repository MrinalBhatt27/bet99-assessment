package com.bet99.bugtracker.repository;

import com.bet99.bugtracker.model.Bug;
import com.bet99.bugtracker.model.BugStatus;
import com.bet99.bugtracker.model.Severity;

import java.util.List;
import java.util.Optional;

public interface BugRepository {
    Bug save(Bug bug);
    Optional<Bug> findById(Long id);
    List<Bug> findAll();
    List<Bug> findBySeverity(Severity severity);
    List<Bug> findByStatus(BugStatus status);
    List<Bug> findBySeverityAndStatus(Severity severity, BugStatus status);
    void deleteById(Long id);
}
