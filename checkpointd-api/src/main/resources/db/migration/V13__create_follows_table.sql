CREATE TABLE follows (
    id UUID PRIMARY KEY,
    follower_id UUID NOT NULL,
    followee_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_follows_follower FOREIGN KEY (follower_id) REFERENCES users (id),
    CONSTRAINT fk_follows_followee FOREIGN KEY (followee_id) REFERENCES users (id),
    CONSTRAINT uk_follows_follower_followee UNIQUE (follower_id, followee_id),
    CONSTRAINT ck_follows_no_self_follow CHECK (follower_id <> followee_id)
);

CREATE INDEX idx_follows_follower_id ON follows (follower_id);
CREATE INDEX idx_follows_followee_id ON follows (followee_id);
