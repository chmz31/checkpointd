CREATE TABLE reviews (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    game_id UUID NOT NULL,
    rating INTEGER NULL,
    body TEXT NOT NULL,
    contains_spoilers BOOLEAN NOT NULL DEFAULT FALSE,
    visibility VARCHAR(40) NOT NULL DEFAULT 'PUBLIC',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_reviews_game FOREIGN KEY (game_id) REFERENCES games (id),
    CONSTRAINT uk_reviews_user_game UNIQUE (user_id, game_id),
    CONSTRAINT ck_reviews_rating_range CHECK (rating IS NULL OR (rating >= 1 AND rating <= 10))
);

CREATE INDEX idx_reviews_game_id ON reviews (game_id);
CREATE INDEX idx_reviews_user_id ON reviews (user_id);
CREATE INDEX idx_reviews_updated_at ON reviews (updated_at);
