package com.apparel.commerce.customer;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {
    List<CustomerAddress> findByCustomerIdOrderByIsDefaultDescIdAsc(String customerId);

    Optional<CustomerAddress> findByIdAndCustomerId(Long id, String customerId);

    long countByCustomerId(String customerId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update CustomerAddress a set a.isDefault = false where a.customerId = :customerId")
    void clearDefault(@Param("customerId") String customerId);
}
