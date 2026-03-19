package com.premiersport.product.repository;

import com.premiersport.product.entity.ProductEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends MongoRepository<ProductEntity, String> {

    Optional<ProductEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<ProductEntity> findByIsFeaturedTrue();
}
