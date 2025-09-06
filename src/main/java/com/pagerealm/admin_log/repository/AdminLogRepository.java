package com.pagerealm.admin_log.repository;

import com.pagerealm.admin_log.entity.AdminLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminLogRepository extends JpaRepository<AdminLog, Long> {
    Page<AdminLog> findByAdminId(Long adminId, Pageable pageable);
}

