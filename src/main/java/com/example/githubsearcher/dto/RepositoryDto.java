package com.example.githubsearcher.dto;

import java.time.Instant;

public record RepositoryDto(
        Long id,
        String name,
        String description,
        String owner,
        String language,
        int stars,
        int forks,
        Instant lastUpdated,
        String url,
        int watchers,
        int openIssues,
        Integer sizeKb,
        String license,
        Instant createdAt,
        Instant pushedAt) {
}
