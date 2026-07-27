package com.chmz31.checkpointd.game.controller;

import com.chmz31.checkpointd.game.dto.CreateGameRequest;
import com.chmz31.checkpointd.game.dto.GameResponse;
import com.chmz31.checkpointd.game.service.GameService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/games")
public class GameController {

	private final GameService gameService;

	public GameController(GameService gameService) {
		this.gameService = gameService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	GameResponse create(@Valid @RequestBody CreateGameRequest request) {
		return GameResponse.from(gameService.create(request));
	}

	@GetMapping
	List<GameResponse> list(@RequestParam(required = false) String q) {
		return gameService.list(q).stream()
				.map(GameResponse::from)
				.toList();
	}

	@GetMapping("/{gameId}")
	GameResponse get(@PathVariable UUID gameId) {
		var game = gameService.get(gameId);
		return GameResponse.from(game, gameService.isMetadataStale(game));
	}
}
