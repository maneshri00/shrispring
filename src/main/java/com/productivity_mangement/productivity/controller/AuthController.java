package com.productivity_mangement.productivity.controller;

import com.productivity_mangement.productivity.service.GoogleOAuthService;
import com.productivity_mangement.productivity.service.UserService;
import com.productivity_mangement.productivity.service.ProfileInferenceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

@RestController
public class AuthController {

    private final GoogleOAuthService oauthService;
    private final UserService userService;
    private final ProfileInferenceService profileInferenceService;

    public AuthController(GoogleOAuthService oauthService, UserService userService, ProfileInferenceService profileInferenceService) {
        this.oauthService = oauthService;
        this.userService = userService;
        this.profileInferenceService = profileInferenceService;
    }

    @GetMapping("/auth/google")
    public void login(
            @RequestParam(required = false, defaultValue = "") String workspace,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        if (!workspace.isEmpty()) {
            request.getSession().setAttribute("workspaceAllowed", workspace);
        }
        response.sendRedirect(oauthService.buildAuthUrl());
    }

    @GetMapping("/auth/google/callback")
    public void callback(
            @RequestParam String code,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException, GeneralSecurityException {

        String email = oauthService.exchangeCode(code);


        boolean isNewUser = userService.handleLogin(email);

        request.getSession().setAttribute("email", email);
        request.getSession().setAttribute("isNewUser", isNewUser);

        try {
            profileInferenceService.inferAndApply(email);
        } catch (Exception ignored) {}

        String allowed = (String) request.getSession().getAttribute("workspaceAllowed");
        String path = "personal";
        if ("professional".equalsIgnoreCase(allowed)) path = "professional";
        response.sendRedirect("http://localhost:5173/" + path);
    }


    @GetMapping("/api/user/status")
    public Map<String, Object> getUserStatus(HttpServletRequest request) {
        Boolean isNewUser = (Boolean) request.getSession().getAttribute("isNewUser");
        String email = (String) request.getSession().getAttribute("email");
        String workspaceAllowed = (String) request.getSession().getAttribute("workspaceAllowed");

        Map<String, Object> response = new HashMap<>();
        response.put("isNewUser", isNewUser != null && isNewUser);
        response.put("loggedIn", email != null);
      response.put("workspaceAllowed", workspaceAllowed);
        return response;
    }
    @PostMapping("/login")
    public String login(@RequestParam String email) {

        boolean firstLogin = userService.handleLogin(email);

        return firstLogin
                ? "Welcome! First login detected"
                : "Welcome back!";
    }

    @PostMapping("/api/auth/logout")
    public Map<String, String> logout(HttpServletRequest request) {
        try {
            request.getSession().invalidate();
        } catch (Exception ignored) {}
        return Map.of("status", "logged_out");
    }


}
