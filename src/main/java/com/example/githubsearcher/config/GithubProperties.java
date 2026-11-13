package com.example.githubsearcher.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Externalised settings for the GitHub REST API client. */
@ConfigurationProperties(prefix = "github.api")
public record GithubProperties(
        @DefaultValue("https://api.github.com") String baseUrl,
        String token,
        @DefaultValue("30") int defaultPerPage,
        @DefaultValue("5s") Duration connectTimeout,
        @DefaultValue("10s") Duration readTimeout) {
}
