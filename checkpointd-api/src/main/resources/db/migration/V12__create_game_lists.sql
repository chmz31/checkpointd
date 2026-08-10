CREATE TABLE game_lists (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    visibility VARCHAR(40) NOT NULL DEFAULT 'PUBLIC',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_game_lists_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_game_lists_user_id ON game_lists (user_id);
CREATE INDEX idx_game_lists_updated_at ON game_lists (updated_at);

CREATE TABLE game_list_items (
    id UUID PRIMARY KEY,
    list_id UUID NOT NULL,
    game_id UUID NOT NULL,
    position INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_game_list_items_list FOREIGN KEY (list_id) REFERENCES game_lists (id) ON DELETE CASCADE,
    CONSTRAINT fk_game_list_items_game FOREIGN KEY (game_id) REFERENCES games (id),
    CONSTRAINT uk_game_list_items_list_game UNIQUE (list_id, game_id)
);

CREATE INDEX idx_game_list_items_list_id ON game_list_items (list_id);
CREATE INDEX idx_game_list_items_game_id ON game_list_items (game_id);
