package com.premiersport.order.service;

import com.premiersport.order.dto.AnalyticsDto;
import com.premiersport.order.entity.CartEntity;
import com.premiersport.order.entity.OrderEntity;
import com.premiersport.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final OrderRepository orderRepository;
    private final MongoTemplate mongoTemplate;

    public AnalyticsDto getAnalytics() {
        Instant startOfToday = Instant.now().truncatedTo(ChronoUnit.DAYS);
        Instant startOfYesterday = startOfToday.minus(1, ChronoUnit.DAYS);

        List<OrderEntity> allOrders = orderRepository.findAll();

        long totalOrders = allOrders.size();
        double totalRevenue = allOrders.stream().mapToDouble(OrderEntity::getTotal).sum();

        List<OrderEntity> todayOrders = allOrders.stream()
                .filter(o -> o.getCreatedAt() != null && !o.getCreatedAt().isBefore(startOfToday))
                .toList();
        long ordersToday = todayOrders.size();
        double revenueToday = todayOrders.stream().mapToDouble(OrderEntity::getTotal).sum();

        List<OrderEntity> yesterdayOrders = allOrders.stream()
                .filter(o -> o.getCreatedAt() != null
                        && !o.getCreatedAt().isBefore(startOfYesterday)
                        && o.getCreatedAt().isBefore(startOfToday))
                .toList();
        long ordersPreviousPeriod = yesterdayOrders.size();
        double revenuePreviousPeriod = yesterdayOrders.stream().mapToDouble(OrderEntity::getTotal).sum();

        List<AnalyticsDto.TopProductDto> topProducts = computeTopProducts(allOrders);

        List<OrderEntity> recentOrders = orderRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();

        return AnalyticsDto.builder()
                .totalOrders(totalOrders)
                .totalRevenue(totalRevenue)
                .ordersToday(ordersToday)
                .revenueToday(revenueToday)
                .ordersPreviousPeriod(ordersPreviousPeriod)
                .revenuePreviousPeriod(revenuePreviousPeriod)
                .topProducts(topProducts)
                .recentOrders(recentOrders)
                .build();
    }

    private List<AnalyticsDto.TopProductDto> computeTopProducts(List<OrderEntity> orders) {
        Map<String, long[]> productStats = new LinkedHashMap<>();
        // productStats[productId] = [totalQty, totalRevenueX100, nameHash]
        Map<String, String> productNames = new HashMap<>();
        Map<String, double[]> productRevenue = new HashMap<>();

        for (OrderEntity order : orders) {
            if (order.getItems() == null) continue;
            for (CartEntity.CartItem item : order.getItems()) {
                String pid = item.getProductId();
                productNames.put(pid, item.getProductName());
                productStats.merge(pid, new long[]{item.getQuantity()},
                        (existing, inc) -> new long[]{existing[0] + inc[0]});
                productRevenue.merge(pid, new double[]{item.getUnitPrice() * item.getQuantity()},
                        (existing, inc) -> new double[]{existing[0] + inc[0]});
            }
        }

        return productStats.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]))
                .limit(5)
                .map(entry -> AnalyticsDto.TopProductDto.builder()
                        .productId(entry.getKey())
                        .productName(productNames.getOrDefault(entry.getKey(), "Unknown"))
                        .totalQuantity(entry.getValue()[0])
                        .totalRevenue(productRevenue.getOrDefault(entry.getKey(), new double[]{0})[0])
                        .build())
                .toList();
    }
}
