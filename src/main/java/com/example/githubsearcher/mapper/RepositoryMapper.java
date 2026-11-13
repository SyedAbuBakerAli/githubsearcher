package com.example.githubsearcher.mapper;

import com.example.githubsearcher.client.dto.GithubRepoItem;
import com.example.githubsearcher.dto.RepositoryDto;
import com.example.githubsearcher.entity.GithubRepository;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class RepositoryMapper {

    public GithubRepository toEntity(GithubRepoItem item) {
        GithubRepository entity = new GithubRepository(item.id());
        apply(entity, item);
        return entity;
    }

    public void update(GithubRepository entity, GithubRepoItem item) {
        apply(entity, item);
    }

    public RepositoryDto toDto(GithubRepository e) {
        return new RepositoryDto(e.getId(), e.getName(), e.getDescription(), e.getOwner(), e.getLanguage(),
                e.getStars(), e.getForks(), e.getLastUpdated(), e.getHtmlUrl(), e.getWatchers(),
                e.getOpenIssues(), e.getSizeKb(), e.getLicense(), e.getCreatedAt(), e.getPushedAt());
    }

    private void apply(GithubRepository e, GithubRepoItem i) {
        e.setName(i.name() != null ? i.name() : "unknown");
        e.setFullName(i.fullName());
        e.setDescription(truncate(i.description(), 1024));
        e.setOwner(i.owner() != null ? i.owner().login() : null);
        e.setLanguage(i.language());
        e.setStars(zeroIfNull(i.stargazersCount()));
        e.setForks(zeroIfNull(i.forksCount()));
        e.setWatchers(zeroIfNull(i.watchersCount()));
        e.setOpenIssues(zeroIfNull(i.openIssuesCount()));
        e.setSizeKb(i.size());
        e.setLicense(i.license() != null ? i.license().name() : null);
        e.setHtmlUrl(i.htmlUrl());
        e.setCreatedAt(i.createdAt());
        e.setPushedAt(i.pushedAt());
        e.setLastUpdated(i.updatedAt());
        e.setLastFetchedAt(Instant.now());
    }

    private static int zeroIfNull(Integer value) {
        return value == null ? 0 : value;
    }

    private static String truncate(String value, int max) {
        return value != null && value.length() > max ? value.substring(0, max) : value;
    }
}
