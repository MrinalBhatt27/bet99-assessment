package com.bet99.bugtracker.controller;

import com.bet99.bugtracker.dto.BugResponse;
import com.bet99.bugtracker.dto.CreateBugRequest;
import com.bet99.bugtracker.model.Severity;
import com.bet99.bugtracker.service.BugService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/bugs")
@Validated
public class BugApiController {

    private final BugService bugService;

    public BugApiController(BugService bugService) {
        this.bugService = bugService;
    }

    @PostMapping
    public ResponseEntity<BugResponse> create(@Valid @RequestBody CreateBugRequest request) {
        return ResponseEntity.ok(bugService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<BugResponse>> list(@RequestParam(name = "severity", required = false) Severity severity) {
        return ResponseEntity.ok(bugService.list(Optional.ofNullable(severity)));
    }
}

