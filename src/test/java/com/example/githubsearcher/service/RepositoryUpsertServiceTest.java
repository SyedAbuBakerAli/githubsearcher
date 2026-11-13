package com.example.githubsearcher.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.githubsearcher.client.dto.GithubRepoItem;
import com.example.githubsearcher.entity.GithubRepository;
import com.example.githubsearcher.mapper.RepositoryMapper;
import com.example.githubsearcher.repository.GithubRepositoryRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({RepositoryUpsertService.class, RepositoryMapper.class})
class RepositoryUpsertServiceTest {

    @Autowired
    private RepositoryUpsertService service;
    @Autowired
    private GithubRepositoryRepository repository;
    @Autowired
    private EntityManager em;

    @Test
    void insertsNewRepositories() {
        List<GithubRepository> saved = service.upsertAll(List.of(
                TestData.item(1, "alpha", "Java", 10, 1, "2024-01-01T00:00:00Z"),
                TestData.item(2, "beta", "Go", 20, 2, "2024-02-01T00:00:00Z")));
        em.flush();
        em.clear();

        assertThat(saved).extracting(GithubRepository::getId).containsExactly(1L, 2L);
        assertThat(repository.count()).isEqualTo(2);
        GithubRepository alpha = repository.findById(1L).orElseThrow();
        assertThat(alpha.getOwner()).isEqualTo("owner");
        assertThat(alpha.getLicense()).isEqualTo("MIT License");
        assertThat(alpha.getLastFetchedAt()).isNotNull();
    }

    @Test
    void updatesExistingRepositoryInsteadOfDuplicating() {
        service.upsertAll(List.of(TestData.item(1, "alpha", "Java", 10, 1, "2024-01-01T00:00:00Z")));
        em.flush();
        em.clear();

        service.upsertAll(List.of(
                TestData.item(1, "alpha-renamed", "Kotlin", 99, 5, "2024-06-01T00:00:00Z"),
                TestData.item(2, "beta", "Go", 20, 2, "2024-02-01T00:00:00Z")));
        em.flush();
        em.clear();

        assertThat(repository.count()).isEqualTo(2);
        GithubRepository updated = repository.findById(1L).orElseThrow();
        assertThat(updated.getName()).isEqualTo("alpha-renamed");
        assertThat(updated.getLanguage()).isEqualTo("Kotlin");
        assertThat(updated.getStars()).isEqualTo(99);
        assertThat(updated.getForks()).isEqualTo(5);
    }

    @Test
    void collapsesDuplicatesWithinOneBatch() {
        List<GithubRepository> saved = service.upsertAll(List.of(
                TestData.item(1, "alpha", "Java", 10, 1, "2024-01-01T00:00:00Z"),
                TestData.item(1, "alpha", "Java", 11, 1, "2024-01-01T00:00:00Z")));
        em.flush();
        em.clear();

        assertThat(saved).hasSize(1);
        assertThat(repository.findById(1L).orElseThrow().getStars()).isEqualTo(11);
    }

    @Test
    void skipsItemsWithoutIdAndHandlesNullFields() {
        GithubRepoItem noId = new GithubRepoItem(null, "x", null, null, null, null, null, null, null, null,
                null, null, null, null, null, null);
        GithubRepoItem sparse = new GithubRepoItem(7L, "sparse", null, null, null, null, null, null, null, null,
                null, null, null, null, null, null);

        List<GithubRepository> saved = service.upsertAll(List.of(noId, sparse));
        em.flush();
        em.clear();

        assertThat(saved).hasSize(1);
        GithubRepository stored = repository.findById(7L).orElseThrow();
        assertThat(stored.getStars()).isZero();
        assertThat(stored.getOwner()).isNull();
    }

    @Test
    void emptyInputIsNoOp() {
        assertThat(service.upsertAll(List.of())).isEmpty();
        assertThat(repository.count()).isZero();
    }
}
