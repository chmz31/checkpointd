package com.chmz31.checkpointd.comment.entity;

import com.chmz31.checkpointd.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "list_comment_reports", uniqueConstraints = {
		@UniqueConstraint(name = "uk_list_comment_reports_comment_reporter", columnNames = {"comment_id", "reporter_id"})
})
public class ListCommentReport {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "comment_id", nullable = false)
	private ListComment comment;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "reporter_id", nullable = false)
	private User reporter;

	@Column(columnDefinition = "TEXT")
	private String reason;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected ListCommentReport() {
	}

	public ListCommentReport(ListComment comment, User reporter, String reason) {
		this.comment = comment;
		this.reporter = reporter;
		this.reason = reason;
	}

	@PrePersist
	void onCreate() {
		this.createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public ListComment getComment() {
		return comment;
	}

	public User getReporter() {
		return reporter;
	}

	public String getReason() {
		return reason;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
