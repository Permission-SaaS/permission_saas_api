package com.saas.permissions.modules.identity.infrastructure;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;


public interface JpaClientRepository extends JpaRepository<ClientJpaEntity, UUID> {

	boolean existsByEmail(String email);

	java.util.Optional<ClientJpaEntity> findByEmail(String email);
}