package com.bet99.bugtracker.controller;

import com.bet99.bugtracker.dto.BugResponse;
import com.bet99.bugtracker.dto.CreateBugRequest;
import com.bet99.bugtracker.dto.UpdateStatusRequest;
import com.bet99.bugtracker.model.BugStatus;
import com.bet99.bugtracker.model.Severity;
import com.bet99.bugtracker.service.BugService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    private static final Logger log = LoggerFactory.getLogger(BugApiController.class);

    private final BugService bugService;

    public BugApiController(BugService bugService) {
        this.bugService = bugService;
    }

    @PostMapping
    public ResponseEntity<BugResponse> create(@Valid @RequestBody CreateBugRequest request) {
        log.info("POST /api/bugs title='{}'", request.getBugTitle());
        return ResponseEntity.ok(bugService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<BugResponse>> list(
            @RequestParam(name = "severity", required = false) Severity severity,
            @RequestParam(name = "status", required = false) BugStatus status) {
        log.debug("GET /api/bugs severity={} status={}", severity, status);
        return ResponseEntity.ok(bugService.list(Optional.ofNullable(severity), Optional.ofNullable(status)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /api/bugs/{}", id);
        bugService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<BugResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {
        log.info("PATCH /api/bugs/{}/status -> {}", id, request.getStatus());
        return ResponseEntity.ok(bugService.updateStatus(id, request.getStatus()));
    }
}
