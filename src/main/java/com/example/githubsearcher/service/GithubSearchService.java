package com.example.githubsearcher.service;

import com.example.githubsearcher.client.GithubApiClient;
import com.example.githubsearcher.client.dto.GithubRepoItem;
import com.example.githubsearcher.client.dto.GithubSearchResponse;
import com.example.githubsearcher.config.GithubProperties;
import com.example.githubsearcher.dto.RepositoryDto;
import com.example.githubsearcher.dto.SearchRequest;
import com.example.githubsearcher.dto.SearchResponse;
import com.example.githubsearcher.mapper.RepositoryMapper;
import com.example.githubsearcher.util.GithubQueryBuilder;
import com.example.githubsearcher.util.SortField;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GithubSearchService {

    static final String MSG_SAVED = "Repositories fetched and saved successfully";
    static final String MSG_EMPTY = "No repositories found for the given criteria";

    private static final Logger log = LoggerFactory.getLogger(GithubSearchService.class);

    private final GithubApiClient client;
    private final RepositoryUpsertService upsertService;
    private final RepositoryMapper mapper;
    private final GithubProperties properties;

    public GithubSearchService(GithubApiClient client, RepositoryUpsertService upsertService,
                               RepositoryMapper mapper, GithubProperties properties) {
        this.client = client;
        this.upsertService = upsertService;
        this.mapper = mapper;
        this.properties = properties;
    }

    public SearchResponse search(SearchRequest request) {
        SortField sort = SortField.parseOrDefault(request.sort(), null);
        String query = GithubQueryBuilder.build(request.query(), request.language());
        int perPage = request.perPage() != null ? request.perPage() : properties.defaultPerPage();
        int page = request.page() != null ? request.page() : 1;

        GithubSearchResponse response = client.searchRepositories(
                query, sort == null ? null : sort.githubValue(), perPage, page);

        List<GithubRepoItem> items = response.items();
        if (items.isEmpty()) {
            log.info("GitHub returned no results for '{}'", query);
            return new SearchResponse(MSG_EMPTY, List.of());
        }

        List<RepositoryDto> saved = upsertService.upsertAll(items).stream().map(mapper::toDto).toList();
        log.info("Upserted {} repositories for '{}'", saved.size(), query);
        return new SearchResponse(MSG_SAVED, saved);
    }
}
