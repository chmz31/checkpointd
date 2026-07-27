ALTER TABLE games
    ADD COLUMN external_rating DOUBLE PRECISION,
    ADD COLUMN external_rating_count INTEGER;

CREATE TABLE game_developers (
    game_id UUID NOT NULL,
    developer VARCHAR(255) NOT NULL,
    developer_order INTEGER NOT NULL,
    CONSTRAINT pk_game_developers PRIMARY KEY (game_id, developer_order),
    CONSTRAINT fk_game_developers_game FOREIGN KEY (game_id) REFERENCES games (id) ON DELETE CASCADE
);

CREATE TABLE game_publishers (
    game_id UUID NOT NULL,
    publisher VARCHAR(255) NOT NULL,
    publisher_order INTEGER NOT NULL,
    CONSTRAINT pk_game_publishers PRIMARY KEY (game_id, publisher_order),
    CONSTRAINT fk_game_publishers_game FOREIGN KEY (game_id) REFERENCES games (id) ON DELETE CASCADE
);

CREATE TABLE game_modes (
    game_id UUID NOT NULL,
    mode VARCHAR(255) NOT NULL,
    mode_order INTEGER NOT NULL,
    CONSTRAINT pk_game_modes PRIMARY KEY (game_id, mode_order),
    CONSTRAINT fk_game_modes_game FOREIGN KEY (game_id) REFERENCES games (id) ON DELETE CASCADE
);

CREATE TABLE game_themes (
    game_id UUID NOT NULL,
    theme VARCHAR(255) NOT NULL,
    theme_order INTEGER NOT NULL,
    CONSTRAINT pk_game_themes PRIMARY KEY (game_id, theme_order),
    CONSTRAINT fk_game_themes_game FOREIGN KEY (game_id) REFERENCES games (id) ON DELETE CASCADE
);

CREATE TABLE game_player_perspectives (
    game_id UUID NOT NULL,
    player_perspective VARCHAR(255) NOT NULL,
    perspective_order INTEGER NOT NULL,
    CONSTRAINT pk_game_player_perspectives PRIMARY KEY (game_id, perspective_order),
    CONSTRAINT fk_game_player_perspectives_game FOREIGN KEY (game_id) REFERENCES games (id) ON DELETE CASCADE
);

CREATE TABLE game_websites (
    game_id UUID NOT NULL,
    website_label VARCHAR(255) NOT NULL,
    website_url VARCHAR(2048) NOT NULL,
    trusted BOOLEAN NOT NULL,
    website_order INTEGER NOT NULL,
    CONSTRAINT pk_game_websites PRIMARY KEY (game_id, website_order),
    CONSTRAINT fk_game_websites_game FOREIGN KEY (game_id) REFERENCES games (id) ON DELETE CASCADE
);
