package com.chmz31.checkpointd.externalgames.service;

import com.chmz31.checkpointd.externalgames.dto.ExternalGameSearchResult;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ExternalGameSearchService {

	private final IgdbClient igdbClient;

	public ExternalGameSearchService(IgdbClient igdbClient) {
		this.igdbClient = igdbClient;
	}

	public List<ExternalGameSearchResult> search(String query) {
		return igdbClient.search(query.trim());
	}
}
