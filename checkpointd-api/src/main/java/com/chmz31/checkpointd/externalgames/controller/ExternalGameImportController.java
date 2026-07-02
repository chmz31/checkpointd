package com.chmz31.checkpointd.externalgames.controller;

import com.chmz31.checkpointd.externalgames.dto.ImportExternalGameRequest;
import com.chmz31.checkpointd.externalgames.service.ExternalGameImportService;
import com.chmz31.checkpointd.externalgames.service.ImportedGameResult;
import com.chmz31.checkpointd.game.dto.GameResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/external-games")
public class ExternalGameImportController {

	private final ExternalGameImportService externalGameImportService;

	public ExternalGameImportController(ExternalGameImportService externalGameImportService) {
		this.externalGameImportService = externalGameImportService;
	}

	@PostMapping("/import")
	ResponseEntity<GameResponse> importGame(@Valid @RequestBody ImportExternalGameRequest request) {
		ImportedGameResult result = externalGameImportService.importGame(request);
		HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;

		return ResponseEntity.status(status).body(GameResponse.from(result.game()));
	}
}
