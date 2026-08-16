package com.nethcare.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables JPA auditing — auto-populates @CreatedDate and @LastModifiedDate
 * on all entities extending BaseEntity.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditConfig {
}
