CREATE TABLE games (
    id UUID PRIMARY KEY,
    external_provider VARCHAR(255),
    external_id VARCHAR(255),
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255),
    cover_url VARCHAR(2048),
    release_date DATE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_games_external_provider_external_id UNIQUE (external_provider, external_id)
);

CREATE TABLE library_entries (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    game_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    rating INTEGER,
    notes TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_library_entries_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_library_entries_game FOREIGN KEY (game_id) REFERENCES games (id),
    CONSTRAINT uk_library_entries_user_game UNIQUE (user_id, game_id)
);
