package com.example.githubsearcher.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SearchRequest(
        @NotBlank(message = "must not be blank")
        @Size(max = 256, message = "must be at most 256 characters")
        String query,

        @Size(max = 50, message = "must be at most 50 characters")
        @Pattern(regexp = "^[A-Za-z0-9+#.\\- ]*$", message = "contains invalid characters")
        String language,

        String sort,

        @Min(value = 1, message = "must be at least 1")
        @Max(value = 100, message = "must be at most 100")
        Integer perPage,

        @Min(value = 1, message = "must be at least 1")
        Integer page) {
}
