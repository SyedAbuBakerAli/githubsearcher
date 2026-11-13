package com.example.githubsearcher.repository;

import com.example.githubsearcher.entity.GithubRepository;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class RepositorySpecifications {

    private RepositorySpecifications() {
    }

    public static Specification<GithubRepository> hasLanguage(String language) {
        return (root, query, cb) -> language == null || language.isBlank()
                ? cb.conjunction()
                : cb.equal(cb.lower(root.get("language")), language.trim().toLowerCase(Locale.ROOT));
    }

    public static Specification<GithubRepository> hasMinStars(Integer minStars) {
        return (root, query, cb) -> minStars == null
                ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.<Integer>get("stars"), minStars);
    }
}
