package com.example.githubsearcher.util;

import java.util.Locale;

/** Builds the "q" parameter of the GitHub repository search from user input. */
public final class GithubQueryBuilder {

    private GithubQueryBuilder() {
    }

    /**
     * "spring boot" + "Java"  ->  spring boot in:name language:Java
     * Matching is restricted to the repository name (partial or full) unless the caller already
     * supplied their own "in:" qualifier.
     */
    public static String build(String query, String language) {
        StringBuilder sb = new StringBuilder(query.trim());
        if (!query.toLowerCase(Locale.ROOT).contains("in:")) {
            sb.append(" in:name");
        }
        if (language != null && !language.isBlank()) {
            String lang = language.trim();
            sb.append(" language:");
            if (lang.contains(" ")) {
                sb.append('"').append(lang).append('"');
            } else {
                sb.append(lang);
            }
        }
        return sb.toString();
    }
}
