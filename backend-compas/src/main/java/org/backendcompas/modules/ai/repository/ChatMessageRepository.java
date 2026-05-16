package org.backendcompas.modules.ai.repository;

import org.backendcompas.modules.ai.model.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    Page<ChatMessage> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<ChatMessage> findTop20ByUserIdOrderByCreatedAtDesc(UUID userId);
}
