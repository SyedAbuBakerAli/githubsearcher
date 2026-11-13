package com.example.githubsearcher.service;

import com.example.githubsearcher.client.dto.GithubRepoItem;
import com.example.githubsearcher.entity.GithubRepository;
import com.example.githubsearcher.mapper.RepositoryMapper;
import com.example.githubsearcher.repository.GithubRepositoryRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RepositoryUpsertService {

    private final GithubRepositoryRepository repository;
    private final RepositoryMapper mapper;

    public RepositoryUpsertService(GithubRepositoryRepository repository, RepositoryMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public List<GithubRepository> upsertAll(List<GithubRepoItem> items) {
        Map<Long, GithubRepoItem> byId = new LinkedHashMap<>();
        for (GithubRepoItem item : items) {
            if (item != null && item.id() != null) {
                byId.put(item.id(), item);
            }
        }
        if (byId.isEmpty()) {
            return List.of();
        }

        Map<Long, GithubRepository> existing = repository.findAllById(byId.keySet()).stream()
                .collect(Collectors.toMap(GithubRepository::getId, Function.identity()));

        List<GithubRepository> toSave = new ArrayList<>(byId.size());
        byId.forEach((id, item) -> {
            GithubRepository entity = existing.get(id);
            if (entity == null) {
                toSave.add(mapper.toEntity(item));
            } else {
                mapper.update(entity, item);
                toSave.add(entity);
            }
        });
        return repository.saveAll(toSave);
    }
}
