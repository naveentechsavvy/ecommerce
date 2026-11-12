package com.apparel.commerce.payment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    Optional<Payment> findFirstByOrderIdAndStatus(Long orderId, Payment.Status status);
}
