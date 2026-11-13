package com.example.githubsearcher.service;

import com.example.githubsearcher.client.dto.GithubRepoItem;
import com.example.githubsearcher.entity.GithubRepository;
import java.time.Instant;

/** Shared builders for tests. */
final class TestData {

    private TestData() {
    }

    static GithubRepoItem item(long id, String name, String language, int stars, int forks, String updatedAt) {
        return new GithubRepoItem(id, name, "owner/" + name, "desc of " + name,
                new GithubRepoItem.Owner("owner"), language, stars, forks, stars, 3,
                "https://github.com/owner/" + name, 100,
                Instant.parse("2020-01-01T00:00:00Z"), Instant.parse(updatedAt),
                Instant.parse(updatedAt), new GithubRepoItem.License("MIT License"));
    }

    static GithubRepository entity(long id, String name, String language, int stars, int forks, String updatedAt) {
        GithubRepository e = new GithubRepository(id);
        e.setName(name);
        e.setOwner("owner");
        e.setLanguage(language);
        e.setStars(stars);
        e.setForks(forks);
        e.setLastUpdated(Instant.parse(updatedAt));
        return e;
    }
}
