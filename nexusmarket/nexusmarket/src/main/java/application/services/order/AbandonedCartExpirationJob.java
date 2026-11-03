package application.services.order;

import application.domain.ports.in.OrderInputPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class AbandonedCartExpirationJob {
    private final OrderInputPort orders;
    private final Duration expiration;

    public AbandonedCartExpirationJob(
            OrderInputPort orders,
            @Value("${nexusmarket.orders.cart-expiration:PT30M}") Duration expiration) {
        if (expiration.isZero() || expiration.isNegative()) {
            throw new IllegalArgumentException("Cart expiration must be positive");
        }
        this.orders = orders;
        this.expiration = expiration;
    }

    @Scheduled(fixedDelayString = "${nexusmarket.orders.expiration-scan-ms:60000}")
    public void expireAbandonedCarts() {
        orders.expireAbandonedOrders(Instant.now().minus(expiration));
    }
}
