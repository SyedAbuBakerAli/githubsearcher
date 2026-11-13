package com.example.githubsearcher.service;

import com.example.githubsearcher.dto.RepositoryListResponse;
import com.example.githubsearcher.entity.GithubRepository;
import com.example.githubsearcher.exception.InvalidRequestException;
import com.example.githubsearcher.mapper.RepositoryMapper;
import com.example.githubsearcher.repository.GithubRepositoryRepository;
import com.example.githubsearcher.repository.RepositorySpecifications;
import com.example.githubsearcher.util.SortField;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RepositoryQueryService {

    public static final int DEFAULT_LIMIT = 100;
    public static final int MAX_LIMIT = 1000;

    private final GithubRepositoryRepository repository;
    private final RepositoryMapper mapper;

    public RepositoryQueryService(GithubRepositoryRepository repository, RepositoryMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public RepositoryListResponse find(String language, Integer minStars, String sort, Integer limit) {
        if (minStars != null && minStars < 0) {
            throw new InvalidRequestException("minStars must be greater than or equal to 0");
        }
        int size = limit == null ? DEFAULT_LIMIT : limit;
        if (size < 1 || size > MAX_LIMIT) {
            throw new InvalidRequestException("limit must be between 1 and " + MAX_LIMIT);
        }
        SortField sortField = SortField.parseOrDefault(sort, SortField.STARS);

        Specification<GithubRepository> spec = Specification
                .where(RepositorySpecifications.hasLanguage(language))
                .and(RepositorySpecifications.hasMinStars(minStars));

        Sort order = Sort.by(Sort.Direction.DESC, sortField.entityProperty())
                .and(Sort.by(Sort.Direction.ASC, "id"));      // deterministic tie-break

        List<GithubRepository> found = repository.findAll(spec, PageRequest.of(0, size, order)).getContent();
        return new RepositoryListResponse(found.stream().map(mapper::toDto).toList());
    }
}
