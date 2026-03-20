package com.premiersport.order.dto;

import com.premiersport.order.entity.OrderEntity;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AnalyticsDto {
    private long totalOrders;
    private double totalRevenue;
    private long ordersToday;
    private double revenueToday;
    private long ordersPreviousPeriod;
    private double revenuePreviousPeriod;
    private List<TopProductDto> topProducts;
    private List<OrderEntity> recentOrders;

    @Data
    @Builder
    public static class TopProductDto {
        private String productId;
        private String productName;
        private long totalQuantity;
        private double totalRevenue;
    }
}
