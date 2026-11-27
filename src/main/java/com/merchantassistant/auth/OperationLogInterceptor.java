package com.merchantassistant.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class OperationLogInterceptor implements HandlerInterceptor {
    private final OperationLogRepository repository;
    public OperationLogInterceptor(OperationLogRepository repository) { this.repository = repository; }
    @Override public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) {
        if (request.getAttribute("auth.user") instanceof SystemUser user && request.getRequestURI().startsWith("/api/") && !"GET".equalsIgnoreCase(request.getMethod())) {
            OperationLog log = new OperationLog(); log.setUsername(user.getUsername()); log.setMethod(request.getMethod()); log.setPath(request.getRequestURI()); log.setStatus(response.getStatus()); repository.save(log);
        }
    }
}
