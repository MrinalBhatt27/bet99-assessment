package com.bet99.bugtracker.dto;

import com.bet99.bugtracker.model.BugStatus;

import javax.validation.constraints.NotNull;

public class UpdateStatusRequest {

    @NotNull(message = "status must not be null")
    private BugStatus status;

    public BugStatus getStatus() {
        return status;
    }

    public void setStatus(BugStatus status) {
        this.status = status;
    }
}
