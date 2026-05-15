package com.connectsphere.report_service.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.connectsphere.report_service.entity.Report;
import com.connectsphere.report_service.entity.Report.ReportStatus;
import com.connectsphere.report_service.entity.Report.TargetType;

// Repository for Report entity.
@Repository
public interface ReportRepository extends JpaRepository<Report, Integer> {

	// Checks if a user has already reported the same target.
	boolean existsByReporterIdAndTargetIdAndTargetType(Integer reporterId, Integer targetId, TargetType targetType);

	// Gets reports by status (used for moderation queue).
	Page<Report> findByStatus(ReportStatus status, Pageable pageable);

	// Gets all reports created by a user.
	Page<Report> findByReporterId(Integer reporterId, Pageable pageable);

	// Counts pending reports for a target since a given time.
	@Query("""
			    SELECT COUNT(r) FROM Report r
			    WHERE r.targetId   = :targetId
			      AND r.targetType = :targetType
			      AND r.status     = 'PENDING'
			      AND r.createdAt >= :since
			""")
	long countPendingReportsForTargetSince(@Param("targetId") Integer targetId,
			@Param("targetType") TargetType targetType, @Param("since") LocalDateTime since);

	// Gets report count grouped by status.
	@Query("SELECT r.status, COUNT(r) FROM Report r GROUP BY r.status")
	List<Object[]> countGroupByStatus();

	// Gets pending reports for a specific target.
	List<Report> findByTargetIdAndTargetTypeAndStatus(Integer targetId, TargetType targetType, ReportStatus status);
}