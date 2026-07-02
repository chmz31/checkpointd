package com.chmz31.checkpointd.externalgames.service;

import com.chmz31.checkpointd.game.entity.Game;

public record ImportedGameResult(Game game, boolean created) {
}
