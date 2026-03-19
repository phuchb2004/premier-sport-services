package com.premiersport.order.repository;

import com.premiersport.order.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends MongoRepository<OrderEntity, String> {
    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<OrderEntity> findByOrderNumber(String orderNumber);
    Page<OrderEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
