package com.oms.order.scheduler;

import com.oms.common.enums.OrderStatus;
import com.oms.order.entity.Order;
import com.oms.order.repository.OrderRepository;
import com.oms.order.service.OrderEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderSchedulerService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Auto-cancel unpaid orders older than 30 minutes.
     * Runs every 15 minutes.
     */
    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    public void autoCancelStaleOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
        List<Order> staleOrders = orderRepository.findStaleOrders(OrderStatus.PENDING, cutoff);

        if (staleOrders.isEmpty()) {
            log.debug("No stale orders to cancel");
            return;
        }

        log.info("Auto-cancelling {} stale unpaid orders", staleOrders.size());
        staleOrders.forEach(order -> {
            order.setStatus(OrderStatus.CANCELLED);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            // Evict from cache
            redisTemplate.delete("order:" + order.getId());

            // Publish status change event
            eventPublisher.publishOrderStatusChanged(
                    order.getId(), order.getUserId(),
                    OrderStatus.PENDING, OrderStatus.CANCELLED
            );

            log.info("Auto-cancelled order id={} (created at {})", order.getId(), order.getCreatedAt());
        });
    }

    /**
     * Log order statistics every hour.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional(readOnly = true)
    public void logOrderStats() {
        long totalOrders = orderRepository.count();
        log.info("Order statistics - Total orders: {}", totalOrders);
    }
}
