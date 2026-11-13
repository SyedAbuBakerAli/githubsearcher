package com.example.githubsearcher.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import org.springframework.data.domain.Persistable;

/**
 * A GitHub repository snapshot. The primary key is GitHub's own repository id, which guarantees
 * uniqueness. Implementing Persistable lets Spring Data issue a plain INSERT for new rows
 * instead of the extra SELECT that merge() would do for an assigned id.
 */
@Entity
@Table(name = "github_repositories", indexes = {
        @Index(name = "idx_repo_language", columnList = "language"),
        @Index(name = "idx_repo_stars", columnList = "stars_count"),
        @Index(name = "idx_repo_forks", columnList = "forks_count"),
        @Index(name = "idx_repo_updated", columnList = "last_updated")
})
public class GithubRepository implements Persistable<Long> {

    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "full_name", length = 512)
    private String fullName;

    @Column(length = 1024)
    private String description;

    @Column(name = "owner_name")
    private String owner;

    @Column(name = "language")
    private String language;

    @Column(name = "stars_count", nullable = false)
    private int stars;

    @Column(name = "forks_count", nullable = false)
    private int forks;

    @Column(name = "watchers_count", nullable = false)
    private int watchers;

    @Column(name = "open_issues_count", nullable = false)
    private int openIssues;

    @Column(name = "size_kb")
    private Integer sizeKb;

    @Column(name = "license_name")
    private String license;

    @Column(name = "html_url", length = 512)
    private String htmlUrl;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "pushed_at")
    private Instant pushedAt;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    @Column(name = "last_fetched_at")
    private Instant lastFetchedAt;

    @Transient
    private boolean newEntity = true;

    protected GithubRepository() {
    }

    public GithubRepository(Long id) {
        this.id = id;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.newEntity = false;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public int getStars() { return stars; }
    public void setStars(int stars) { this.stars = stars; }
    public int getForks() { return forks; }
    public void setForks(int forks) { this.forks = forks; }
    public int getWatchers() { return watchers; }
    public void setWatchers(int watchers) { this.watchers = watchers; }
    public int getOpenIssues() { return openIssues; }
    public void setOpenIssues(int openIssues) { this.openIssues = openIssues; }
    public Integer getSizeKb() { return sizeKb; }
    public void setSizeKb(Integer sizeKb) { this.sizeKb = sizeKb; }
    public String getLicense() { return license; }
    public void setLicense(String license) { this.license = license; }
    public String getHtmlUrl() { return htmlUrl; }
    public void setHtmlUrl(String htmlUrl) { this.htmlUrl = htmlUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getPushedAt() { return pushedAt; }
    public void setPushedAt(Instant pushedAt) { this.pushedAt = pushedAt; }
    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
    public Instant getLastFetchedAt() { return lastFetchedAt; }
    public void setLastFetchedAt(Instant lastFetchedAt) { this.lastFetchedAt = lastFetchedAt; }
}
