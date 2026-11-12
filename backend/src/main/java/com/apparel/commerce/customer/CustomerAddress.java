package com.apparel.commerce.customer;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Saved address book entry. Customer profile data (name, email, password, MFA)
 * lives in Keycloak; the commerce DB only keys data by the Keycloak subject.
 */
@Entity
@Table(name = "customer_address")
@Getter
@Setter
public class CustomerAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerId;
    private String label;
    private String fullName;
    private String phone;
    private String line1;
    private String line2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private boolean isDefault;
}
