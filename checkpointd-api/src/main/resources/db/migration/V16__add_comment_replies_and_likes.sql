ALTER TABLE list_comments ADD COLUMN parent_id UUID REFERENCES list_comments (id) ON DELETE CASCADE;
CREATE INDEX idx_list_comments_parent_id ON list_comments (parent_id);

ALTER TABLE review_comments ADD COLUMN parent_id UUID REFERENCES review_comments (id) ON DELETE CASCADE;
CREATE INDEX idx_review_comments_parent_id ON review_comments (parent_id);

CREATE TABLE list_comment_likes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    comment_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_list_comment_likes_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_list_comment_likes_comment FOREIGN KEY (comment_id) REFERENCES list_comments (id) ON DELETE CASCADE,
    CONSTRAINT uk_list_comment_likes_user_comment UNIQUE (user_id, comment_id)
);

CREATE INDEX idx_list_comment_likes_comment_id ON list_comment_likes (comment_id);

CREATE TABLE review_comment_likes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    comment_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_review_comment_likes_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_review_comment_likes_comment FOREIGN KEY (comment_id) REFERENCES review_comments (id) ON DELETE CASCADE,
    CONSTRAINT uk_review_comment_likes_user_comment UNIQUE (user_id, comment_id)
);

CREATE INDEX idx_review_comment_likes_comment_id ON review_comment_likes (comment_id);
