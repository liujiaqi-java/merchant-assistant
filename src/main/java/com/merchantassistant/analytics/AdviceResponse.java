package com.merchantassistant.analytics;

import java.time.Instant;
import java.util.List;

public record AdviceResponse(String source, String statusMessage, String summary, List<String> actions, Instant generatedAt) {
}
