package com.example.githubsearcher.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.githubsearcher.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

class SortFieldTest {

    @Test
    void returnsDefaultWhenValueMissing() {
        assertThat(SortField.parseOrDefault(null, SortField.STARS)).isEqualTo(SortField.STARS);
        assertThat(SortField.parseOrDefault("  ", SortField.FORKS)).isEqualTo(SortField.FORKS);
        assertThat(SortField.parseOrDefault(null, null)).isNull();
    }

    @Test
    void parsesCaseInsensitively() {
        assertThat(SortField.parseOrDefault("STARS", null)).isEqualTo(SortField.STARS);
        assertThat(SortField.parseOrDefault("Forks", null)).isEqualTo(SortField.FORKS);
        assertThat(SortField.parseOrDefault("updated", null)).isEqualTo(SortField.UPDATED);
    }

    @Test
    void mapsToEntityPropertyAndGithubValue() {
        assertThat(SortField.UPDATED.entityProperty()).isEqualTo("lastUpdated");
        assertThat(SortField.UPDATED.githubValue()).isEqualTo("updated");
    }

    @Test
    void rejectsUnknownValue() {
        assertThatThrownBy(() -> SortField.parseOrDefault("watchers", null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Allowed values: stars, forks, updated");
    }
}
