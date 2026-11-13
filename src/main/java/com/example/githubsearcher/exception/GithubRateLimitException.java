package com.example.githubsearcher.exception;

public class GithubRateLimitException extends RuntimeException {
    private final Long retryAfterSeconds;

    public GithubRateLimitException(String message, Long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
