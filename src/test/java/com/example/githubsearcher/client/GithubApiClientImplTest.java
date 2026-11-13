package com.example.githubsearcher.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.githubsearcher.client.dto.GithubSearchResponse;
import com.example.githubsearcher.exception.GithubApiException;
import com.example.githubsearcher.exception.GithubRateLimitException;
import com.example.githubsearcher.exception.InvalidRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GithubApiClientImplTest {

    private static final String BASE = "https://api.github.com";

    private static final String SAMPLE = """
            {
              "total_count": 1,
              "incomplete_results": false,
              "items": [{
                "id": 123456,
                "name": "spring-boot-example",
                "full_name": "user123/spring-boot-example",
                "description": "An example repository for Spring Boot",
                "owner": {"login": "user123", "id": 1},
                "language": "Java",
                "stargazers_count": 450,
                "forks_count": 120,
                "watchers_count": 450,
                "open_issues_count": 7,
                "html_url": "https://github.com/user123/spring-boot-example",
                "size": 2048,
                "created_at": "2020-01-01T00:00:00Z",
                "updated_at": "2024-01-01T12:00:00Z",
                "pushed_at": "2024-01-01T11:00:00Z",
                "license": {"name": "MIT License"},
                "some_unknown_field": true
              }]
            }
            """;

    private MockRestServiceServer server;
    private GithubApiClientImpl client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GithubApiClientImpl(builder.build());
    }

    @Test
    void parsesSuccessfulResponse() {
        server.expect(requestTo(startsWith(BASE + "/search/repositories")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("sort", "stars"))
                .andExpect(queryParam("per_page", "10"))
                .andExpect(queryParam("page", "2"))
                .andRespond(withSuccess(SAMPLE, MediaType.APPLICATION_JSON));

        GithubSearchResponse response = client.searchRepositories("spring in:name language:Java", "stars", 10, 2);

        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.items()).hasSize(1);
        var item = response.items().get(0);
        assertThat(item.id()).isEqualTo(123456L);
        assertThat(item.owner().login()).isEqualTo("user123");
        assertThat(item.stargazersCount()).isEqualTo(450);
        assertThat(item.updatedAt()).hasToString("2024-01-01T12:00:00Z");
        assertThat(item.license().name()).isEqualTo("MIT License");
        server.verify();
    }

    @Test
    void omitsSortParameterWhenNull() {
        server.expect(requestTo(startsWith(BASE + "/search/repositories")))
                .andRespond(withSuccess("{\"total_count\":0,\"items\":[]}", MediaType.APPLICATION_JSON));

        assertThat(client.searchRepositories("x", null, 30, 1).items()).isEmpty();
        server.verify();
    }

    @Test
    void mapsRateLimitedForbiddenResponse() {
        server.expect(requestTo(startsWith(BASE)))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .header("X-RateLimit-Remaining", "0")
                        .header("Retry-After", "60")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"API rate limit exceeded\"}"));

        assertThatThrownBy(() -> client.searchRepositories("x", "stars", 30, 1))
                .isInstanceOf(GithubRateLimitException.class)
                .satisfies(ex -> assertThat(((GithubRateLimitException) ex).getRetryAfterSeconds()).isEqualTo(60L));
    }

    @Test
    void mapsTooManyRequests() {
        server.expect(requestTo(startsWith(BASE))).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> client.searchRepositories("x", "stars", 30, 1))
                .isInstanceOf(GithubRateLimitException.class)
                .satisfies(ex -> assertThat(((GithubRateLimitException) ex).getRetryAfterSeconds()).isNull());
    }

    @Test
    void plainForbiddenIsNotTreatedAsRateLimit() {
        server.expect(requestTo(startsWith(BASE))).andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> client.searchRepositories("x", "stars", 30, 1))
                .isExactlyInstanceOf(GithubApiException.class);
    }

    @Test
    void mapsValidationFailedToInvalidRequest() {
        server.expect(requestTo(startsWith(BASE))).andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));

        assertThatThrownBy(() -> client.searchRepositories("x", "stars", 30, 1))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void mapsServerErrorToGithubApiException() {
        server.expect(requestTo(startsWith(BASE))).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.searchRepositories("x", "stars", 30, 1))
                .isExactlyInstanceOf(GithubApiException.class)
                .satisfies(ex -> assertThat(((GithubApiException) ex).getUpstreamStatus()).isEqualTo(500));
    }

    @Test
    void mapsUnauthorized() {
        server.expect(requestTo(startsWith(BASE))).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.searchRepositories("x", "stars", 30, 1))
                .isExactlyInstanceOf(GithubApiException.class)
                .hasMessageContaining("authentication");
    }

    @Test
    void rejectsMalformedJson() {
        server.expect(requestTo(startsWith(BASE)))
                .andRespond(withSuccess("this is not json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.searchRepositories("x", "stars", 30, 1))
                .isExactlyInstanceOf(GithubApiException.class);
    }

    @Test
    void rejectsResponseWithoutItems() {
        server.expect(requestTo(startsWith(BASE)))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.searchRepositories("x", "stars", 30, 1))
                .isExactlyInstanceOf(GithubApiException.class)
                .hasMessageContaining("unexpected response");
    }
}
