package com.example.githubsearcher.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.githubsearcher.dto.RepositoryDto;
import com.example.githubsearcher.dto.RepositoryListResponse;
import com.example.githubsearcher.dto.SearchResponse;
import com.example.githubsearcher.exception.GithubApiException;
import com.example.githubsearcher.exception.GithubRateLimitException;
import com.example.githubsearcher.exception.InvalidRequestException;
import com.example.githubsearcher.service.GithubSearchService;
import com.example.githubsearcher.service.RepositoryQueryService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GithubRepositoryController.class)
class GithubRepositoryControllerTest {

    @Autowired
    private MockMvc mvc;
    @MockBean
    private GithubSearchService searchService;
    @MockBean
    private RepositoryQueryService queryService;

    private static RepositoryDto dto() {
        return new RepositoryDto(123456L, "spring-boot-example", "An example repository for Spring Boot",
                "user123", "Java", 450, 120, Instant.parse("2024-01-01T12:00:00Z"),
                "https://github.com/user123/spring-boot-example", 450, 3, 100, "MIT License",
                Instant.parse("2020-01-01T00:00:00Z"), Instant.parse("2024-01-01T11:00:00Z"));
    }

    @Test
    void searchReturnsSavedRepositories() throws Exception {
        when(searchService.search(any())).thenReturn(
                new SearchResponse("Repositories fetched and saved successfully", List.of(dto())));

        mvc.perform(post("/api/github/search").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"spring boot\",\"language\":\"Java\",\"sort\":\"stars\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Repositories fetched and saved successfully"))
                .andExpect(jsonPath("$.repositories[0].id").value(123456))
                .andExpect(jsonPath("$.repositories[0].owner").value("user123"))
                .andExpect(jsonPath("$.repositories[0].stars").value(450))
                .andExpect(jsonPath("$.repositories[0].forks").value(120))
                .andExpect(jsonPath("$.repositories[0].lastUpdated").value("2024-01-01T12:00:00Z"));
    }

    @Test
    void searchRejectsBlankQuery() throws Exception {
        mvc.perform(post("/api/github/search").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.details[0]").value("query: must not be blank"));
    }

    @Test
    void searchRejectsOutOfRangePerPageAndBadLanguage() throws Exception {
        mvc.perform(post("/api/github/search").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"x\",\"language\":\"Ja\\\"va\",\"perPage\":500}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.length()").value(2));
    }

    @Test
    void searchRejectsMalformedJson() throws Exception {
        mvc.perform(post("/api/github/search").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or missing JSON request body"));
    }

    @Test
    void searchRejectsUnsupportedMediaType() throws Exception {
        mvc.perform(post("/api/github/search").contentType(MediaType.TEXT_PLAIN).content("hi"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void searchMapsInvalidSortTo400() throws Exception {
        when(searchService.search(any())).thenThrow(new InvalidRequestException("Invalid sort value 'x'"));

        mvc.perform(post("/api/github/search").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"x\",\"sort\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid sort value 'x'"));
    }

    @Test
    void searchMapsRateLimitTo429WithRetryAfter() throws Exception {
        when(searchService.search(any())).thenThrow(new GithubRateLimitException("rate limited", 42L));

        mvc.perform(post("/api/github/search").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"x\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "42"))
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    void searchMapsUpstreamFailureTo502() throws Exception {
        when(searchService.search(any())).thenThrow(new GithubApiException("GitHub API returned HTTP 500", 500));

        mvc.perform(post("/api/github/search").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"x\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("Bad Gateway"));
    }

    @Test
    void listPassesFiltersToService() throws Exception {
        when(queryService.find("Java", 100, "stars", null)).thenReturn(new RepositoryListResponse(List.of(dto())));

        mvc.perform(get("/api/github/repositories").param("language", "Java").param("minStars", "100")
                        .param("sort", "stars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositories.length()").value(1))
                .andExpect(jsonPath("$.repositories[0].name").value("spring-boot-example"));
        verify(queryService).find("Java", 100, "stars", null);
    }

    @Test
    void listWorksWithoutParameters() throws Exception {
        when(queryService.find(null, null, null, null)).thenReturn(new RepositoryListResponse(List.of()));

        mvc.perform(get("/api/github/repositories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositories").isEmpty());
    }

    @Test
    void listRejectsNonNumericMinStars() throws Exception {
        mvc.perform(get("/api/github/repositories").param("minStars", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parameter 'minStars' has an invalid value: 'abc'"));
    }

    @Test
    void listMapsInvalidSortTo400() throws Exception {
        when(queryService.find(any(), any(), any(), any())).thenThrow(new InvalidRequestException("bad sort"));

        mvc.perform(get("/api/github/repositories").param("sort", "nope"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownPathKeepsNotFoundStatus() throws Exception {
        mvc.perform(get("/api/github/nothing")).andExpect(status().isNotFound());
    }
}
