package com.productivity_mangement.productivity.controller;

import com.productivity_mangement.productivity.entity.IntegrationConnection;
import com.productivity_mangement.productivity.repository.IntegrationConnectionRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

@RestController
public class IntegrationController {

    private static final Set<String> SUPPORTED = Set.of(
            "github", "discord", "slack", "jira", "notion"
    );

    private final IntegrationConnectionRepository repo;

    public IntegrationController(IntegrationConnectionRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/api/integrations")
    public List<Map<String, Object>> list(
            @RequestParam(required = false, defaultValue = "personal") String workspace,
            HttpServletRequest request
    ) {
        String email = (String) request.getSession().getAttribute("email");
        if (email == null) {
            return Collections.emptyList();
        }
        List<IntegrationConnection> items = repo.findByUserEmailAndWorkspace(email, workspace);
        List<Map<String, Object>> out = new ArrayList<>();
        for (IntegrationConnection ic : items) {
            Map<String, Object> m = new HashMap<>();
            m.put("provider", ic.getProvider());
            m.put("workspace", ic.getWorkspace());
            m.put("status", ic.getStatus());
            m.put("connected_at", ic.getConnectedAt() != null ? ic.getConnectedAt().toString() : null);
            out.add(m);
        }
        return out;
    }

    @DeleteMapping("/api/integrations")
    public Map<String, Object> disconnectAll(
            @RequestParam(required = false, defaultValue = "personal") String workspace,
            HttpServletRequest request
    ) {
        String email = (String) request.getSession().getAttribute("email");
        if (email == null) {
            return Map.of("status", "error", "message", "not_logged_in");
        }
        List<IntegrationConnection> items = repo.findByUserEmailAndWorkspace(email, workspace);
        for (IntegrationConnection ic : items) {
            repo.deleteByUserEmailAndWorkspaceAndProvider(email, workspace, ic.getProvider());
        }
        return Map.of("status", "disconnected_all", "workspace", workspace);
    }

    @DeleteMapping("/api/integrations/{provider}")
    public Map<String, Object> disconnect(
            @PathVariable String provider,
            @RequestParam(required = false, defaultValue = "personal") String workspace,
            HttpServletRequest request
    ) {
        String email = (String) request.getSession().getAttribute("email");
        if (email == null) {
            return Map.of("status", "error", "message", "not_logged_in");
        }
        repo.deleteByUserEmailAndWorkspaceAndProvider(email, workspace, provider);
        return Map.of("status", "disconnected", "provider", provider, "workspace", workspace);
    }

    @GetMapping("/auth/oauth/{provider}/initiate")
    public void oauthInitiate(
            @PathVariable String provider,
            @RequestParam(required = false, defaultValue = "personal") String workspace,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        if (!SUPPORTED.contains(provider)) {
            response.sendError(400, "Unsupported provider");
            return;
        }
        try {
            request.getSession(true).setAttribute("workspaceAllowed", workspace);
        } catch (Exception ignored) {}
        String email = (String) request.getSession().getAttribute("email");
        if (email != null) {
            IntegrationConnection ic = repo
                    .findByUserEmailAndWorkspaceAndProvider(email, workspace, provider)
                    .orElseGet(IntegrationConnection::new);
            ic.setUserEmail(email);
            ic.setWorkspace(workspace);
            ic.setProvider(provider);
            ic.setStatus("connected");
            ic.setConnectedAt(Instant.now());
            repo.save(ic);
        }
        response.sendRedirect("http://localhost:5173/integrations/" + provider + "?connected=true&workspace=" + workspace);
    }

    @GetMapping("/auth/slack")
    public void slackAlias(
            @RequestParam(required = false, defaultValue = "personal") String workspace,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        try {
            request.getSession(true).setAttribute("workspaceAllowed", workspace);
        } catch (Exception ignored) {}
        String email = (String) request.getSession().getAttribute("email");
        if (email != null) {
            IntegrationConnection ic = repo
                    .findByUserEmailAndWorkspaceAndProvider(email, workspace, "slack")
                    .orElseGet(IntegrationConnection::new);
            ic.setUserEmail(email);
            ic.setWorkspace(workspace);
            ic.setProvider("slack");
            ic.setStatus("connected");
            ic.setConnectedAt(Instant.now());
            repo.save(ic);
        }
        response.sendRedirect("http://localhost:5173/integrations/slack?connected=true&workspace=" + workspace);
    }

    @GetMapping("/auth/oauth/{provider}/callback")
    public void oauthCallback(
        @PathVariable String provider,
        @RequestParam(required = false, defaultValue = "personal") String workspace,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws IOException {
        if (!SUPPORTED.contains(provider)) {
            response.sendError(400, "Unsupported provider");
            return;
        }
        try {
            request.getSession(true).setAttribute("workspaceAllowed", workspace);
        } catch (Exception ignored) {}
        String email = (String) request.getSession().getAttribute("email");
        if (email != null) {
            IntegrationConnection ic = repo
                    .findByUserEmailAndWorkspaceAndProvider(email, workspace, provider)
                    .orElseGet(IntegrationConnection::new);
            ic.setUserEmail(email);
            ic.setWorkspace(workspace);
            ic.setProvider(provider);
            ic.setStatus("connected");
            ic.setConnectedAt(Instant.now());
            repo.save(ic);
        }
        response.sendRedirect("http://localhost:5173/integrations/" + provider + "?connected=true&workspace=" + workspace);
    }

    @PostMapping("/api/integrations/connect")
    public Map<String, Object> connectManual(
            @RequestParam String provider,
            @RequestParam(required = false, defaultValue = "personal") String workspace,
            HttpServletRequest request
    ) {
        String email = (String) request.getSession().getAttribute("email");
        if (email == null) {
            return Map.of("status", "error", "message", "not_logged_in");
        }
        IntegrationConnection ic = repo
                .findByUserEmailAndWorkspaceAndProvider(email, workspace, provider)
                .orElseGet(IntegrationConnection::new);
        ic.setUserEmail(email);
        ic.setWorkspace(workspace);
        ic.setProvider(provider);
        ic.setStatus("connected");
        ic.setConnectedAt(Instant.now());
        repo.save(ic);
        return Map.of(
                "provider", provider,
                "workspace", workspace,
                "status", "connected",
                "connected_at", ic.getConnectedAt().toString()
        );
    }
}
