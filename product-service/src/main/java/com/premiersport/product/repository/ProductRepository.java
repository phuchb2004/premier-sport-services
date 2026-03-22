package com.premiersport.product.repository;

import com.premiersport.product.entity.ProductEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.data.domain.Pageable;

@Repository
public interface ProductRepository extends MongoRepository<ProductEntity, String> {

    boolean existsBySlug(String slug);

    @Query("{ 'isFeatured': true, 'isDeleted': { $ne: true } }")
    List<ProductEntity> findByIsFeaturedTrueAndIsDeletedFalse(Pageable pageable);
}
