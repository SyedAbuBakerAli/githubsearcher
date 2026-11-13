package com.example.githubsearcher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.githubsearcher.client.GithubApiClient;
import com.example.githubsearcher.client.dto.GithubRepoItem;
import com.example.githubsearcher.client.dto.GithubSearchResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** End-to-end through controller, services and (H2) database; only the GitHub HTTP client is mocked. */
@SpringBootTest
@AutoConfigureMockMvc
class GithubRepositoryApiIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @MockBean
    private GithubApiClient githubApiClient;

    private static GithubRepoItem item(long id, String name, int stars) {
        return new GithubRepoItem(id, name, "o/" + name, "d", new GithubRepoItem.Owner("o"), "Java", stars, 1, stars, 0,
                "https://github.com/o/" + name, 10, Instant.parse("2020-01-01T00:00:00Z"),
                Instant.parse("2024-01-01T00:00:00Z"), Instant.parse("2024-01-01T00:00:00Z"), null);
    }

    @Test
    void searchTwiceUpsertsAndGetReturnsLatestValues() throws Exception {
        when(githubApiClient.searchRepositories(any(), any(), anyInt(), anyInt()))
                .thenReturn(new GithubSearchResponse(2, false, List.of(item(9001, "first", 100), item(9002, "second", 300))))
                .thenReturn(new GithubSearchResponse(1, false, List.of(item(9001, "first", 700))));

        String body = "{\"query\":\"demo\",\"language\":\"Java\",\"sort\":\"stars\"}";
        mvc.perform(post("/api/github/search").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositories.length()").value(2));
        mvc.perform(post("/api/github/search").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        mvc.perform(get("/api/github/repositories").param("language", "java").param("minStars", "200").param("sort", "stars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositories.length()").value(2))
                .andExpect(jsonPath("$.repositories[0].id").value(9001))
                .andExpect(jsonPath("$.repositories[0].stars").value(700))
                .andExpect(jsonPath("$.repositories[1].id").value(9002));
    }
}
