CREATE TABLE list_likes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    list_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_list_likes_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_list_likes_list FOREIGN KEY (list_id) REFERENCES game_lists (id) ON DELETE CASCADE,
    CONSTRAINT uk_list_likes_user_list UNIQUE (user_id, list_id)
);

CREATE INDEX idx_list_likes_list_id ON list_likes (list_id);
CREATE INDEX idx_list_likes_user_id ON list_likes (user_id);

CREATE TABLE review_likes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    review_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_review_likes_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_review_likes_review FOREIGN KEY (review_id) REFERENCES reviews (id) ON DELETE CASCADE,
    CONSTRAINT uk_review_likes_user_review UNIQUE (user_id, review_id)
);

CREATE INDEX idx_review_likes_review_id ON review_likes (review_id);
CREATE INDEX idx_review_likes_user_id ON review_likes (user_id);
