package com.example.githubsearcher.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GithubQueryBuilderTest {

    @Test
    void restrictsToNameByDefault() {
        assertThat(GithubQueryBuilder.build("  spring boot ", null)).isEqualTo("spring boot in:name");
    }

    @Test
    void appendsLanguageQualifier() {
        assertThat(GithubQueryBuilder.build("spring", "Java")).isEqualTo("spring in:name language:Java");
    }

    @Test
    void quotesLanguagesContainingSpaces() {
        assertThat(GithubQueryBuilder.build("demo", "Jupyter Notebook"))
                .isEqualTo("demo in:name language:\"Jupyter Notebook\"");
    }

    @Test
    void keepsCallerSuppliedInQualifier() {
        assertThat(GithubQueryBuilder.build("spring in:readme", "Java")).isEqualTo("spring in:readme language:Java");
    }

    @Test
    void ignoresBlankLanguage() {
        assertThat(GithubQueryBuilder.build("spring", "  ")).isEqualTo("spring in:name");
    }
}
