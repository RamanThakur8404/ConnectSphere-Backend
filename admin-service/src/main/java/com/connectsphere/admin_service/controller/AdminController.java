package com.connectsphere.admin_service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.connectsphere.admin_service.dto.AuditLogResponse;
import com.connectsphere.admin_service.dto.CreateAuditLogRequest;
import com.connectsphere.admin_service.service.AdminService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Administrative operations - ADMIN role required")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);
    private final AdminService adminService;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Admin Service is running");
    }

    @Operation(summary = "Get all audit logs", 
               security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLogResponse>> getAllAuditLogs(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching all audit logs");
        return ResponseEntity.ok(adminService.getAllAuditLogs(pageable));
    }

    @Operation(summary = "Create an audit log entry",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> createAuditLog(
            Authentication authentication,
            @Valid @RequestBody CreateAuditLogRequest request) {
        Long adminUserId = extractAdminUserId(authentication);
        String adminUsername = authentication.getName();
        adminService.logAdminAction(
                adminUserId,
                adminUsername,
                request.getAction(),
                request.getDetails(),
                request.getTargetType(),
                request.getTargetId(),
                request.getStatus());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get audit logs by admin user",
               security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/logs/admin/{adminUserId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogsByAdminUser(
            @PathVariable Long adminUserId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching audit logs for admin user: {}", adminUserId);
        return ResponseEntity.ok(adminService.getAuditLogsByAdminUser(adminUserId, pageable));
    }

    @Operation(summary = "Get audit logs by target type (USER, POST, COMMENT, etc)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/logs/target/{targetType}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogsByTargetType(
            @PathVariable String targetType,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching audit logs by target type: {}", targetType);
        return ResponseEntity.ok(adminService.getAuditLogsByTargetType(targetType, pageable));
    }

    @Operation(summary = "Get audit logs by action",
               security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/logs/action/{action}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogsByAction(
            @PathVariable String action,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching audit logs by action: {}", action);
        return ResponseEntity.ok(adminService.getAuditLogsByAction(action, pageable));
    }

    @Operation(summary = "Dashboard - System Overview",
               security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getDashboard(Authentication authentication) {
        log.info("Admin dashboard accessed by: {}", authentication.getName());
        
        return ResponseEntity.ok(new Object() {
            public final String message = "Welcome to Admin Dashboard";
            public final String adminUser = authentication.getName();
            public final String[] features = {
                "User Management",
                "Content Moderation",
                "Report Management",
                "System Analytics",
                "Audit Logging"
            };
        });
    }

    private Long extractAdminUserId(Authentication authentication) {
        if (authentication != null && authentication.getDetails() instanceof String userId) {
            try {
                return Long.parseLong(userId);
            } catch (NumberFormatException ignored) {
                log.warn("Could not parse admin user id from authentication details: {}", userId);
            }
        }
        return 0L;
    }
}
