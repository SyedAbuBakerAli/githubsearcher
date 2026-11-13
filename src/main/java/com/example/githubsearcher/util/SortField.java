package com.example.githubsearcher.util;

import com.example.githubsearcher.exception.InvalidRequestException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Strategy-style enum that knows how a sort option translates to the GitHub API
 * and to the persisted entity, so no caller needs a switch statement.
 */
public enum SortField {
    STARS("stars", "stars", "stars"),
    FORKS("forks", "forks", "forks"),
    UPDATED("updated", "updated", "lastUpdated");

    private final String apiValue;
    private final String githubValue;
    private final String entityProperty;

    SortField(String apiValue, String githubValue, String entityProperty) {
        this.apiValue = apiValue;
        this.githubValue = githubValue;
        this.entityProperty = entityProperty;
    }

    public String apiValue() {
        return apiValue;
    }

    /** Value of the "sort" parameter for api.github.com/search/repositories. */
    public String githubValue() {
        return githubValue;
    }

    /** Name of the JPA entity property to sort on. */
    public String entityProperty() {
        return entityProperty;
    }

    public static SortField parseOrDefault(String value, SortField defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        String candidate = value.trim();
        return Arrays.stream(values())
                .filter(f -> f.apiValue.equalsIgnoreCase(candidate))
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException("Invalid sort value '" + value
                        + "'. Allowed values: "
                        + Arrays.stream(values()).map(SortField::apiValue).collect(Collectors.joining(", "))));
    }
}
