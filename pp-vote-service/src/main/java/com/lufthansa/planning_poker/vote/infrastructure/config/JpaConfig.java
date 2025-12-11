package com.lufthansa.planning_poker.vote.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA configuration class.
 * 
 * This is separated from the main application class to prevent
 * JPA auditing from being loaded during @WebMvcTest slice tests,
 * which would cause "JPA metamodel must not be empty" errors.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}

