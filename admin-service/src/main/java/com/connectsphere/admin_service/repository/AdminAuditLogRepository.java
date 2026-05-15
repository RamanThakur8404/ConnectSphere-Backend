package com.connectsphere.admin_service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.connectsphere.admin_service.entity.AdminAuditLog;

@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
	Page<AdminAuditLog> findByAdminUserId(Long adminUserId, Pageable pageable);

	Page<AdminAuditLog> findByTargetType(String targetType, Pageable pageable);

	Page<AdminAuditLog> findByAction(String action, Pageable pageable);
}
