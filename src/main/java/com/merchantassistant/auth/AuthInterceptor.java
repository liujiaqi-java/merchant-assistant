package com.merchantassistant.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final AuthService authService;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    public AuthInterceptor(AuthService authService, ObjectMapper objectMapper, @Value("${app.security.enabled:true}") boolean enabled) { this.authService = authService; this.objectMapper = objectMapper; this.enabled = enabled; }
    @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        boolean publicAuth = path.equals("/api/auth/login") || path.equals("/api/auth/register")
                || path.equals("/api/auth/recover-password");
        if (!enabled || !path.startsWith("/api/") || publicAuth || HttpMethod.OPTIONS.matches(request.getMethod())) return true;
        try {
            SystemUser user = authService.require(request.getHeader("X-Session-Token"));
            if (isAdminOnly(request) && user.getRole() != UserRole.ADMIN) { writeError(response, 403, "没有执行该操作的权限"); return false; }
            request.setAttribute("auth.user", user); return true;
        } catch (IllegalArgumentException exception) { writeError(response, 401, "登录已过期，请重新登录"); return false; }
    }
    private void writeError(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(new com.merchantassistant.common.ApiError(java.time.Instant.now(), status, message)));
    }
    private boolean isAdminOnly(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean categoryWrite = path.startsWith("/api/categories") && !HttpMethod.GET.matches(request.getMethod());
        boolean productImport = path.equals("/api/data-transfer/products/import");
        return HttpMethod.DELETE.matches(request.getMethod()) || path.endsWith("/cancel")
                || path.equals("/api/operation-logs") || path.startsWith("/api/users")
                || path.startsWith("/api/inventory-adjustments")
                || path.equals("/api/analytics/store-comparison") || categoryWrite || productImport;
    }
}
