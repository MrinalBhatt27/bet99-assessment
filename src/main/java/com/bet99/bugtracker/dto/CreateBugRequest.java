package com.bet99.bugtracker.dto;

import com.bet99.bugtracker.model.BugStatus;
import com.bet99.bugtracker.model.Severity;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class CreateBugRequest {

    @NotBlank
    @Size(max = 255, message = "bugTitle must be 255 characters or fewer")
    private String bugTitle;

    @NotBlank
    private String description;

    @NotNull
    private Severity severity;

    @NotNull
    private BugStatus status;

    public String getBugTitle() {
        return bugTitle;
    }

    public void setBugTitle(String bugTitle) {
        this.bugTitle = bugTitle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public BugStatus getStatus() {
        return status;
    }

    public void setStatus(BugStatus status) {
        this.status = status;
    }
}

