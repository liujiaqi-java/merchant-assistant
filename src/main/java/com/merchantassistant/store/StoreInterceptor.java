package com.merchantassistant.store;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.merchantassistant.common.ApiError;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.time.Instant;

@Component
public class StoreInterceptor implements HandlerInterceptor {
    private final StoreService service;
    private final ObjectMapper objectMapper;
    public StoreInterceptor(StoreService service, ObjectMapper objectMapper) { this.service = service; this.objectMapper = objectMapper; }
    @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!request.getRequestURI().startsWith("/api/") || request.getRequestURI().startsWith("/api/stores") || request.getRequestURI().startsWith("/api/auth")) return true;
        String value = request.getHeader("X-Store-Id");
        long id;
        try {
            id = value == null || value.isBlank() ? StoreContext.DEFAULT_STORE_ID : Long.parseLong(value);
        } catch (NumberFormatException exception) {
            writeError(response, 400, "门店 ID 必须是数字");
            return false;
        }
        try {
            service.require(id); StoreContext.set(id); return true;
        } catch (IllegalArgumentException exception) {
            writeError(response, 400, exception.getMessage());
            return false;
        }
    }
    private void writeError(HttpServletResponse response, int status, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(new ApiError(Instant.now(), status, message)));
    }
    @Override public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) { StoreContext.clear(); }
}
