package com.productivity_mangement.productivity.repository;

import com.productivity_mangement.productivity.entity.IntegrationConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IntegrationConnectionRepository extends JpaRepository<IntegrationConnection, Long> {
    List<IntegrationConnection> findByUserEmailAndWorkspace(String userEmail, String workspace);
    Optional<IntegrationConnection> findByUserEmailAndWorkspaceAndProvider(String userEmail, String workspace, String provider);
    void deleteByUserEmailAndWorkspaceAndProvider(String userEmail, String workspace, String provider);
}

