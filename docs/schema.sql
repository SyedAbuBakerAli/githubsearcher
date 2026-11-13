-- Reference DDL (PostgreSQL). Hibernate creates the same structure with ddl-auto=update.
CREATE TABLE IF NOT EXISTS github_repositories (
    id               BIGINT PRIMARY KEY,               -- GitHub repository id (unique)
    name             VARCHAR(255) NOT NULL,
    full_name        VARCHAR(512),
    description      VARCHAR(1024),
    owner_name       VARCHAR(255),
    language         VARCHAR(255),
    stars_count      INTEGER NOT NULL DEFAULT 0,
    forks_count      INTEGER NOT NULL DEFAULT 0,
    watchers_count   INTEGER NOT NULL DEFAULT 0,
    open_issues_count INTEGER NOT NULL DEFAULT 0,
    size_kb          INTEGER,
    license_name     VARCHAR(255),
    html_url         VARCHAR(512),
    created_at       TIMESTAMP WITH TIME ZONE,
    pushed_at        TIMESTAMP WITH TIME ZONE,
    last_updated     TIMESTAMP WITH TIME ZONE,
    last_fetched_at  TIMESTAMP WITH TIME ZONE
);
CREATE INDEX IF NOT EXISTS idx_repo_language ON github_repositories (language);
CREATE INDEX IF NOT EXISTS idx_repo_stars    ON github_repositories (stars_count);
CREATE INDEX IF NOT EXISTS idx_repo_forks    ON github_repositories (forks_count);
CREATE INDEX IF NOT EXISTS idx_repo_updated  ON github_repositories (last_updated);
