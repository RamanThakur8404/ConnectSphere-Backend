package com.connectsphere.report_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// Report entity representing a user-submitted content moderation report.
@Entity
@Table(
    name = "reports",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_reporter_target",
        columnNames = {"reporter_id", "target_id", "target_type"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer reportId;

    // userId of the authenticated user submitting the report. 
    @Column(name = "reporter_id", nullable = false)
    private Integer reporterId;

    // Primary key of the reported entity (postId, commentId, or userId). 
    @Column(name = "target_id", nullable = false)
    private Integer targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private TargetType targetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    private ReportReason reason;

    // Optional free-text from the reporter — max 500 chars. 
    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    // userId of the admin/moderator who actioned this report. 
    @Column(name = "resolved_by")
    private Integer resolvedBy;

    // Narrative note written by the admin/mod on resolution. 
    @Column(name = "resolution_note", length = 1000)
    private String resolutionNote;

    // AI-generated analysis of the report — populated asynchronously
    @Column(name = "ai_analysis", columnDefinition = "TEXT")
    private String aiAnalysis;

    // Severity score 1-10 produced by the AI analysis pipeline. 
    @Column(name = "ai_severity_score")
    private Integer aiSeverityScore;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    // Enums

    public enum TargetType { POST, COMMENT, USER }

    public enum ReportReason {
        SPAM, HARASSMENT, HATE_SPEECH, NSFW, MISINFORMATION, OTHER
    }

    public enum ReportStatus {
        PENDING, UNDER_REVIEW, RESOLVED, DISMISSED
    }
}
