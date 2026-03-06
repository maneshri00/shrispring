package com.productivity_mangement.productivity.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class WorkspaceAccessInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        if (session == null) return true;

        String email = (String) session.getAttribute("email");
        if (email == null) return true;

        String allowed = (String) session.getAttribute("workspaceAllowed");
        if (allowed == null || allowed.isBlank()) return true;

        String reqWorkspace = request.getParameter("workspace");
        if (reqWorkspace == null || reqWorkspace.isBlank()) {
            reqWorkspace = request.getParameter("workspace_id");
        }

        if (reqWorkspace == null || reqWorkspace.isBlank()) {
            String uri = request.getRequestURI();
            if (uri.contains("/professional")) reqWorkspace = "professional";
            else if (uri.contains("/personal")) reqWorkspace = "personal";
        }

        if (reqWorkspace != null && !reqWorkspace.isBlank() && !allowed.equals(reqWorkspace)) {
            response.sendError(403, "Workspace access denied");
            return false;
        }
        return true;
    }
}

