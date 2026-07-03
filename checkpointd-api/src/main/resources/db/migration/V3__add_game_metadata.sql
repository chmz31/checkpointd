ALTER TABLE games
    ADD COLUMN summary TEXT;

CREATE TABLE game_genres (
    game_id UUID NOT NULL,
    genre VARCHAR(255) NOT NULL,
    genre_order INTEGER NOT NULL,
    CONSTRAINT pk_game_genres PRIMARY KEY (game_id, genre_order),
    CONSTRAINT fk_game_genres_game FOREIGN KEY (game_id) REFERENCES games (id) ON DELETE CASCADE
);

CREATE TABLE game_platforms (
    game_id UUID NOT NULL,
    platform VARCHAR(255) NOT NULL,
    platform_order INTEGER NOT NULL,
    CONSTRAINT pk_game_platforms PRIMARY KEY (game_id, platform_order),
    CONSTRAINT fk_game_platforms_game FOREIGN KEY (game_id) REFERENCES games (id) ON DELETE CASCADE
);
