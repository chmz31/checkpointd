package com.chmz31.checkpointd.game.entity;

import org.hibernate.Hibernate;

// Loads each collection via its own query instead of one multi-join (which multiplies rows combinatorially).
public final class GameCollections {

	private GameCollections() {
	}

	public static Game hydrate(Game game) {
		if (game == null) {
			return null;
		}

		Hibernate.initialize(game.getGenres());
		Hibernate.initialize(game.getPlatforms());
		Hibernate.initialize(game.getDevelopers());
		Hibernate.initialize(game.getPublishers());
		Hibernate.initialize(game.getGameModes());
		Hibernate.initialize(game.getThemes());
		Hibernate.initialize(game.getPlayerPerspectives());
		Hibernate.initialize(game.getWebsites());
		Hibernate.initialize(game.getScreenshotUrls());
		Hibernate.initialize(game.getArtworkUrls());

		return game;
	}
}
