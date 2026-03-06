package com.productivity_mangement.productivity.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "integration_connections",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"userEmail", "workspace", "provider"})
        }
)
public class IntegrationConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private String workspace; // personal | professional

    @Column(nullable = false)
    private String provider; // github | slack | discord | jira | notion

    @Column(nullable = false)
    private String status; // connected | disconnected

    @Column
    private Instant connectedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getConnectedAt() {
        return connectedAt;
    }

    public void setConnectedAt(Instant connectedAt) {
        this.connectedAt = connectedAt;
    }
}

