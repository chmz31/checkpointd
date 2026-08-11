CREATE TABLE list_comments (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    list_id UUID NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_list_comments_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_list_comments_list FOREIGN KEY (list_id) REFERENCES game_lists (id) ON DELETE CASCADE
);

CREATE INDEX idx_list_comments_list_id ON list_comments (list_id);

CREATE TABLE list_comment_reports (
    id UUID PRIMARY KEY,
    comment_id UUID NOT NULL,
    reporter_id UUID NOT NULL,
    reason TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_list_comment_reports_comment FOREIGN KEY (comment_id) REFERENCES list_comments (id) ON DELETE CASCADE,
    CONSTRAINT fk_list_comment_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users (id),
    CONSTRAINT uk_list_comment_reports_comment_reporter UNIQUE (comment_id, reporter_id)
);

CREATE INDEX idx_list_comment_reports_comment_id ON list_comment_reports (comment_id);

CREATE TABLE review_comments (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    review_id UUID NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_review_comments_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_review_comments_review FOREIGN KEY (review_id) REFERENCES reviews (id) ON DELETE CASCADE
);

CREATE INDEX idx_review_comments_review_id ON review_comments (review_id);

CREATE TABLE review_comment_reports (
    id UUID PRIMARY KEY,
    comment_id UUID NOT NULL,
    reporter_id UUID NOT NULL,
    reason TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_review_comment_reports_comment FOREIGN KEY (comment_id) REFERENCES review_comments (id) ON DELETE CASCADE,
    CONSTRAINT fk_review_comment_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users (id),
    CONSTRAINT uk_review_comment_reports_comment_reporter UNIQUE (comment_id, reporter_id)
);

CREATE INDEX idx_review_comment_reports_comment_id ON review_comment_reports (comment_id);
