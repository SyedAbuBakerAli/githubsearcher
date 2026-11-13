package com.example.githubsearcher.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubRepoItem(
        Long id,
        String name,
        @JsonProperty("full_name") String fullName,
        String description,
        Owner owner,
        String language,
        @JsonProperty("stargazers_count") Integer stargazersCount,
        @JsonProperty("forks_count") Integer forksCount,
        @JsonProperty("watchers_count") Integer watchersCount,
        @JsonProperty("open_issues_count") Integer openIssuesCount,
        @JsonProperty("html_url") String htmlUrl,
        Integer size,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt,
        @JsonProperty("pushed_at") Instant pushedAt,
        License license) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Owner(String login) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record License(String name) {
    }
}
