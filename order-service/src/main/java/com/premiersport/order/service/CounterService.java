package com.premiersport.order.service;

import com.premiersport.order.entity.CounterDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CounterService {

    private final MongoTemplate mongoTemplate;

    /**
     * Atomically increments and returns the next sequence value for the given counter name.
     * Uses MongoDB findAndModify with upsert to ensure crash-safe, persistent counters.
     */
    public long getNextSequence(String counterName) {
        Query query = new Query(Criteria.where("_id").is(counterName));
        Update update = new Update().inc("seq", 1L);
        FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(true).upsert(true);
        CounterDocument counter = mongoTemplate.findAndModify(query, update, options, CounterDocument.class);
        return counter != null ? counter.getSeq() : 1L;
    }
}
