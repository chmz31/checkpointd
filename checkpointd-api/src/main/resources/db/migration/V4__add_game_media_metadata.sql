ALTER TABLE games
    ADD COLUMN backdrop_url VARCHAR(2048);

CREATE TABLE game_screenshots (
    game_id UUID NOT NULL,
    screenshot_url VARCHAR(2048) NOT NULL,
    screenshot_order INTEGER NOT NULL,
    CONSTRAINT pk_game_screenshots PRIMARY KEY (game_id, screenshot_order),
    CONSTRAINT fk_game_screenshots_game FOREIGN KEY (game_id) REFERENCES games (id) ON DELETE CASCADE
);

CREATE TABLE game_artworks (
    game_id UUID NOT NULL,
    artwork_url VARCHAR(2048) NOT NULL,
    artwork_order INTEGER NOT NULL,
    CONSTRAINT pk_game_artworks PRIMARY KEY (game_id, artwork_order),
    CONSTRAINT fk_game_artworks_game FOREIGN KEY (game_id) REFERENCES games (id) ON DELETE CASCADE
);
