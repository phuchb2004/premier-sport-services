package com.premiersport.order.repository;

import com.premiersport.order.entity.CartEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CartRepository extends MongoRepository<CartEntity, String> {
    Optional<CartEntity> findByUserId(String userId);
}
