package com.merchantassistant.auth;

import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/operation-logs")
public class OperationLogController {
    private final OperationLogRepository repository;
    public OperationLogController(OperationLogRepository repository) { this.repository = repository; }
    @GetMapping public List<OperationLogView> list() { return repository.findTop100ByOrderByCreatedAtDesc().stream().map(OperationLogView::from).toList(); }
    record OperationLogView(Long id, String username, String method, String path, Integer status, Instant createdAt) {
        static OperationLogView from(OperationLog log) { return new OperationLogView(log.getId(), log.getUsername(), log.getMethod(), log.getPath(), log.getStatus(), log.getCreatedAt()); }
    }
}
