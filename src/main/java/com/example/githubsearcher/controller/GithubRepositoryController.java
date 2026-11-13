package com.example.githubsearcher.controller;

import com.example.githubsearcher.dto.RepositoryListResponse;
import com.example.githubsearcher.dto.SearchRequest;
import com.example.githubsearcher.dto.SearchResponse;
import com.example.githubsearcher.service.GithubSearchService;
import com.example.githubsearcher.service.RepositoryQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/github", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "GitHub repositories")
public class GithubRepositoryController {

    private final GithubSearchService searchService;
    private final RepositoryQueryService queryService;

    public GithubRepositoryController(GithubSearchService searchService, RepositoryQueryService queryService) {
        this.searchService = searchService;
        this.queryService = queryService;
    }

    @Operation(summary = "Search GitHub and store (upsert) the results",
            description = "Calls api.github.com/search/repositories, saves every result in PostgreSQL "
                    + "(existing repositories are updated, never duplicated) and returns them.")
    @PostMapping(path = "/search", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SearchResponse> search(@Valid @RequestBody SearchRequest request) {
        return ResponseEntity.ok(searchService.search(request));
    }

    @Operation(summary = "Retrieve stored repositories",
            description = "All parameters are optional. Results are sorted descending by the chosen field.")
    @GetMapping("/repositories")
    public ResponseEntity<RepositoryListResponse> list(
            @Parameter(description = "Filter by programming language (case-insensitive)")
            @RequestParam(required = false) String language,
            @Parameter(description = "Minimum stars count (>= 0)")
            @RequestParam(required = false) Integer minStars,
            @Parameter(description = "stars (default), forks or updated")
            @RequestParam(required = false) String sort,
            @Parameter(description = "Maximum number of results, 1-1000 (default 100)")
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(queryService.find(language, minStars, sort, limit));
    }
}
