package com.chmz31.checkpointd.game.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class GameWebsite {

	@Column(name = "website_label", nullable = false)
	private String label;

	@Column(name = "website_url", nullable = false, length = 2048)
	private String url;

	@Column(name = "trusted", nullable = false)
	private boolean trusted;

	protected GameWebsite() {
	}

	public GameWebsite(String label, String url, boolean trusted) {
		this.label = label;
		this.url = url;
		this.trusted = trusted;
	}

	public String getLabel() {
		return label;
	}

	public String getUrl() {
		return url;
	}

	public boolean isTrusted() {
		return trusted;
	}
}
