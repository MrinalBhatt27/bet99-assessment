package com.bet99.bugtracker.repository;

import com.bet99.bugtracker.model.Bug;
import com.bet99.bugtracker.model.Severity;

import java.util.List;

public interface BugRepository {
    Bug save(Bug bug);
    List<Bug> findAll();
    List<Bug> findBySeverity(Severity severity);
}

