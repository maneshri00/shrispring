package com.productivity_mangement.productivity.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final WorkspaceAccessInterceptor workspaceAccessInterceptor;

    public WebConfig(WorkspaceAccessInterceptor workspaceAccessInterceptor) {
        this.workspaceAccessInterceptor = workspaceAccessInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(workspaceAccessInterceptor)
                .addPathPatterns(
                        "/api/**",
                        "/tasks/**",
                        "/notifications/**",
                        "/dashboards/**",
                        "/commands/**"
                );
    }
}

