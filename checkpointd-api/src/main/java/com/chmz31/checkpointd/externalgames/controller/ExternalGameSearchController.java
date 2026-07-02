package com.chmz31.checkpointd.externalgames.controller;

import com.chmz31.checkpointd.externalgames.dto.ExternalGameSearchResult;
import com.chmz31.checkpointd.externalgames.service.ExternalGameSearchService;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/external-games")
public class ExternalGameSearchController {

	private final ExternalGameSearchService externalGameSearchService;

	public ExternalGameSearchController(ExternalGameSearchService externalGameSearchService) {
		this.externalGameSearchService = externalGameSearchService;
	}

	@GetMapping("/search")
	List<ExternalGameSearchResult> search(@RequestParam @NotBlank String q) {
		return externalGameSearchService.search(q);
	}
}
