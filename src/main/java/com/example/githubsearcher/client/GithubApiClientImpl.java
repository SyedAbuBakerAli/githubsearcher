package com.example.githubsearcher.client;

import com.example.githubsearcher.client.dto.GithubSearchResponse;
import com.example.githubsearcher.exception.GithubApiException;
import com.example.githubsearcher.exception.GithubRateLimitException;
import com.example.githubsearcher.exception.InvalidRequestException;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class GithubApiClientImpl implements GithubApiClient {

    private static final Logger log = LoggerFactory.getLogger(GithubApiClientImpl.class);

    private final RestClient restClient;

    public GithubApiClientImpl(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public GithubSearchResponse searchRepositories(String query, String sort, int perPage, int page) {
        log.debug("Searching GitHub: q='{}', sort={}, perPage={}, page={}", query, sort, perPage, page);
        try {
            GithubSearchResponse body = restClient.get()
                    .uri(b -> {
                        b.path("/search/repositories")
                                .queryParam("q", "{q}")
                                .queryParam("order", "desc")
                                .queryParam("per_page", perPage)
                                .queryParam("page", page);
                        if (sort != null) {
                            b.queryParam("sort", sort);
                        }
                        return b.build(Map.of("q", query));
                    })
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw translate(response);
                    })
                    .body(GithubSearchResponse.class);

            if (body == null || body.items() == null) {
                throw new GithubApiException("GitHub returned an unexpected response (no 'items' field)", 200);
            }
            return body;
        } catch (ResourceAccessException ex) {
            throw new GithubApiException("Unable to reach the GitHub API: " + ex.getMessage(), ex);
        } catch (RestClientException ex) {
            throw new GithubApiException("GitHub returned a response that could not be read", ex);
        }
    }

    private RuntimeException translate(ClientHttpResponse response) throws IOException {
        int status = response.getStatusCode().value();
        HttpHeaders headers = response.getHeaders();
        String remaining = headers.getFirst("X-RateLimit-Remaining");
        String retryAfter = headers.getFirst(HttpHeaders.RETRY_AFTER);
        String reset = headers.getFirst("X-RateLimit-Reset");

        boolean rateLimited = status == 429
                || (status == 403 && ("0".equals(remaining) || retryAfter != null));
        if (rateLimited) {
            return new GithubRateLimitException(
                    "GitHub API rate limit exceeded. Please retry later"
                            + " or configure a GITHUB_TOKEN for a higher limit.",
                    retryAfterSeconds(retryAfter, reset));
        }
        return switch (status) {
            case 422 -> new InvalidRequestException(
                    "GitHub rejected the search query (HTTP 422). Check the query and language values.");
            case 401 -> new GithubApiException("GitHub authentication failed. Check the configured GITHUB_TOKEN.", status);
            case 403 -> new GithubApiException("GitHub denied the request (HTTP 403).", status);
            default -> new GithubApiException("GitHub API returned HTTP " + status, status);
        };
    }

    private Long retryAfterSeconds(String retryAfter, String reset) {
        try {
            if (retryAfter != null) {
                return Long.parseLong(retryAfter.trim());
            }
            if (reset != null) {
                return Math.max(0, Long.parseLong(reset.trim()) - Instant.now().getEpochSecond());
            }
        } catch (NumberFormatException ignored) {
            // fall through: unknown
        }
        return null;
    }
}
