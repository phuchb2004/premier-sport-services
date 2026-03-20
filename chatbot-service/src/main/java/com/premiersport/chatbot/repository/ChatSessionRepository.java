package com.premiersport.chatbot.repository;

import com.premiersport.chatbot.entity.ChatSessionEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatSessionRepository extends MongoRepository<ChatSessionEntity, String> {
}
