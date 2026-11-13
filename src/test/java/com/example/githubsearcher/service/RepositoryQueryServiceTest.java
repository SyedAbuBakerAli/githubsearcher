package com.example.githubsearcher.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.githubsearcher.dto.RepositoryDto;
import com.example.githubsearcher.exception.InvalidRequestException;
import com.example.githubsearcher.mapper.RepositoryMapper;
import com.example.githubsearcher.repository.GithubRepositoryRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({RepositoryQueryService.class, RepositoryMapper.class})
class RepositoryQueryServiceTest {

    @Autowired
    private RepositoryQueryService service;
    @Autowired
    private GithubRepositoryRepository repository;

    @BeforeEach
    void seed() {
        repository.saveAll(List.of(
                TestData.entity(1, "a", "Java", 500, 10, "2024-03-01T00:00:00Z"),
                TestData.entity(2, "b", "Java", 50, 300, "2024-01-01T00:00:00Z"),
                TestData.entity(3, "c", "Python", 900, 20, "2024-02-01T00:00:00Z"),
                TestData.entity(4, "d", null, 100, 5, "2024-04-01T00:00:00Z")));
    }

    private List<Long> ids(List<RepositoryDto> dtos) {
        return dtos.stream().map(RepositoryDto::id).toList();
    }

    @Test
    void returnsEverythingSortedByStarsByDefault() {
        assertThat(ids(service.find(null, null, null, null).repositories())).containsExactly(3L, 1L, 4L, 2L);
    }

    @Test
    void filtersByLanguageIgnoringCase() {
        assertThat(ids(service.find("jAvA", null, null, null).repositories())).containsExactly(1L, 2L);
    }

    @Test
    void filtersByMinStars() {
        assertThat(ids(service.find(null, 100, "stars", null).repositories())).containsExactly(3L, 1L, 4L);
    }

    @Test
    void combinesLanguageAndMinStars() {
        assertThat(ids(service.find("Java", 100, null, null).repositories())).containsExactly(1L);
    }

    @Test
    void sortsByForks() {
        assertThat(ids(service.find(null, null, "forks", null).repositories())).containsExactly(2L, 3L, 1L, 4L);
    }

    @Test
    void sortsByUpdatedDate() {
        assertThat(ids(service.find(null, null, "updated", null).repositories())).containsExactly(4L, 1L, 3L, 2L);
    }

    @Test
    void honoursLimit() {
        assertThat(ids(service.find(null, null, null, 2).repositories())).containsExactly(3L, 1L);
    }

    @Test
    void returnsEmptyListWhenNothingMatches() {
        assertThat(service.find("Rust", null, null, null).repositories()).isEmpty();
    }

    @Test
    void mapsAllFieldsToDto() {
        RepositoryDto dto = service.find("Python", null, null, null).repositories().get(0);
        assertThat(dto.name()).isEqualTo("c");
        assertThat(dto.owner()).isEqualTo("owner");
        assertThat(dto.stars()).isEqualTo(900);
        assertThat(dto.forks()).isEqualTo(20);
        assertThat(dto.lastUpdated()).hasToString("2024-02-01T00:00:00Z");
    }

    @Test
    void rejectsInvalidArguments() {
        assertThatThrownBy(() -> service.find(null, -1, null, null)).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.find(null, null, "bogus", null)).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.find(null, null, null, 0)).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.find(null, null, null, 1001)).isInstanceOf(InvalidRequestException.class);
    }
}
