package com.apparel.commerce.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    Page<Order> findByCustomerIdOrderByCreatedAtDesc(String customerId, Pageable pageable);

    @Query("""
        select o from Order o
        where (:status is null or o.status = :status)
          and (:q is null or lower(o.orderNumber) like lower(concat('%', cast(:q as String), '%'))
                          or lower(o.email) like lower(concat('%', cast(:q as String), '%')))
        order by o.createdAt desc
        """)
    Page<Order> search(@Param("status") OrderStatus status, @Param("q") String q, Pageable pageable);

    long countByCreatedAtAfter(Instant since);

    long countByStatus(OrderStatus status);

    @Query("select coalesce(sum(o.grandTotal), 0) from Order o where o.createdAt >= :since and o.status in :statuses")
    BigDecimal revenueSince(@Param("since") Instant since, @Param("statuses") Collection<OrderStatus> statuses);

    List<Order> findTop10ByOrderByCreatedAtDesc();
}
