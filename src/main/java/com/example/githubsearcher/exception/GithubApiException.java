package com.example.githubsearcher.exception;

public class GithubApiException extends RuntimeException {
    private final int upstreamStatus;

    public GithubApiException(String message, int upstreamStatus) {
        super(message);
        this.upstreamStatus = upstreamStatus;
    }

    public GithubApiException(String message, Throwable cause) {
        super(message, cause);
        this.upstreamStatus = 0;
    }

    public int getUpstreamStatus() {
        return upstreamStatus;
    }
}
