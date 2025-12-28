package com.hunesion.drool_v2.interceptor;

import com.hunesion.drool_v2.model.AccessResult;
import com.hunesion.drool_v2.service.AccessControlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Set;

/**
 * AccessControlInterceptor - Intercepts HTTP requests and evaluates access policies
 * 
 * This interceptor:
 * - Extracts user info from request header (X-Username)
 * - Evaluates access against Drools policies
 * - Allows or denies the request based on policy result
 */
@Component
public class AccessControlInterceptor implements HandlerInterceptor {

    private final AccessControlService accessControlService;

    // Endpoints that bypass access control (public endpoints)
    private static final Set<String> PUBLIC_ENDPOINTS = Set.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/public",
            "/error"
    );

    // Endpoints for admin policy management (only ADMIN role)
    private static final Set<String> ADMIN_ONLY_PATTERNS = Set.of(
            "/api/policies"
    );

    @Autowired
    public AccessControlInterceptor(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) 
            throws IOException {
        
        String endpoint = request.getRequestURI();
        String method = request.getMethod();

        // Skip OPTIONS requests (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // Skip public endpoints
        if (isPublicEndpoint(endpoint)) {
            return true;
        }

        // Get username from header (in real app, this would come from JWT/session)
        String username = request.getHeader("X-Username");
        
        if (username == null || username.isEmpty()) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, 
                    "Missing X-Username header", null);
            return false;
        }

        // Evaluate access
        AccessResult result = accessControlService.checkAccess(username, endpoint, method);

        if (result.isAllowed()) {
            System.out.println("✓ Access granted for " + username + " to " + method + " " + endpoint);
            return true;
        } else {
            System.out.println("✗ Access denied for " + username + " to " + method + " " + endpoint 
                    + " - Reason: " + result.getDenialReason());
            sendError(response, HttpServletResponse.SC_FORBIDDEN, 
                    result.getDenialReason(), result.getMatchedPolicyName());
            return false;
        }
    }

    private boolean isPublicEndpoint(String endpoint) {
        for (String publicEndpoint : PUBLIC_ENDPOINTS) {
            if (endpoint.startsWith(publicEndpoint)) {
                return true;
            }
        }
        return false;
    }

    private void sendError(HttpServletResponse response, int status, String message, String policy) 
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        String json = String.format(
                "{\"error\": \"%s\", \"status\": %d, \"policy\": \"%s\"}", 
                message, status, policy != null ? policy : "N/A"
        );
        response.getWriter().write(json);
    }
}
