package com.bet99.bugtracker.exception;

public class BugNotFoundException extends RuntimeException {
    public BugNotFoundException(Long id) {
        super("Bug not found: id=" + id);
    }
}
