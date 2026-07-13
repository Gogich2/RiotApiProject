package org.main.refresh.entity;

public enum RefreshState {
    QUEUED,
    RUNNING,
    COMPLETED,
    RATE_LIMITED,
    FAILED
}
