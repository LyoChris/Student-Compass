package org.backendcompas.modules.ai.client.dto;

import java.util.List;

public record AiChatRequest(AiChatContext context, List<AiHistoryEntry> history, String message) {
}
