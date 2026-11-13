package com.example.githubsearcher.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.githubsearcher.client.GithubApiClient;
import com.example.githubsearcher.client.dto.GithubRepoItem;
import com.example.githubsearcher.client.dto.GithubSearchResponse;
import com.example.githubsearcher.config.GithubProperties;
import com.example.githubsearcher.dto.SearchRequest;
import com.example.githubsearcher.dto.SearchResponse;
import com.example.githubsearcher.exception.GithubApiException;
import com.example.githubsearcher.exception.GithubRateLimitException;
import com.example.githubsearcher.exception.InvalidRequestException;
import com.example.githubsearcher.mapper.RepositoryMapper;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GithubSearchServiceTest {

    @Mock
    private GithubApiClient client;
    @Mock
    private RepositoryUpsertService upsertService;

    private final RepositoryMapper mapper = new RepositoryMapper();
    private GithubSearchService service;

    @BeforeEach
    void setUp() {
        GithubProperties props = new GithubProperties("https://api.github.com", null, 30,
                Duration.ofSeconds(5), Duration.ofSeconds(10));
        service = new GithubSearchService(client, upsertService, mapper, props);
    }

    @Test
    void fetchesAndSavesRepositories() {
        GithubRepoItem item = TestData.item(123456, "spring-boot-example", "Java", 450, 120, "2024-01-01T12:00:00Z");
        when(client.searchRepositories("spring boot in:name language:Java", "stars", 30, 1))
                .thenReturn(new GithubSearchResponse(1, false, List.of(item)));
        when(upsertService.upsertAll(List.of(item))).thenReturn(List.of(mapper.toEntity(item)));

        SearchResponse response = service.search(new SearchRequest("spring boot", "Java", "stars", null, null));

        assertThat(response.message()).isEqualTo("Repositories fetched and saved successfully");
        assertThat(response.repositories()).hasSize(1);
        assertThat(response.repositories().get(0).stars()).isEqualTo(450);
        assertThat(response.repositories().get(0).owner()).isEqualTo("owner");
    }

    @Test
    void usesRequestedPaging() {
        when(client.searchRepositories("x in:name", null, 50, 3))
                .thenReturn(new GithubSearchResponse(0, false, List.of()));

        service.search(new SearchRequest("x", null, null, 50, 3));

        verify(client).searchRepositories("x in:name", null, 50, 3);
    }

    @Test
    void returnsFriendlyMessageAndSkipsDatabaseWhenNothingFound() {
        when(client.searchRepositories(any(), any(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(new GithubSearchResponse(0, false, List.of()));

        SearchResponse response = service.search(new SearchRequest("nothing-here", null, "stars", null, null));

        assertThat(response.message()).isEqualTo("No repositories found for the given criteria");
        assertThat(response.repositories()).isEmpty();
        verifyNoInteractions(upsertService);
    }

    @Test
    void rejectsInvalidSortBeforeCallingGithub() {
        assertThatThrownBy(() -> service.search(new SearchRequest("x", null, "bogus", null, null)))
                .isInstanceOf(InvalidRequestException.class);
        verifyNoInteractions(client, upsertService);
    }

    @Test
    void propagatesRateLimitAndDoesNotTouchDatabase() {
        when(client.searchRepositories(any(), any(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt()))
                .thenThrow(new GithubRateLimitException("limit", 30L));

        assertThatThrownBy(() -> service.search(new SearchRequest("x", null, null, null, null)))
                .isInstanceOf(GithubRateLimitException.class);
        verifyNoInteractions(upsertService);
    }

    @Test
    void propagatesUpstreamFailure() {
        when(client.searchRepositories(any(), any(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt()))
                .thenThrow(new GithubApiException("boom", 500));

        assertThatThrownBy(() -> service.search(new SearchRequest("x", null, null, null, null)))
                .isInstanceOf(GithubApiException.class);
    }
}
