package com.example.githubsearcher.client;

import com.example.githubsearcher.client.dto.GithubSearchResponse;

public interface GithubApiClient {


    GithubSearchResponse searchRepositories(String query, String sort, int perPage, int page);
}
