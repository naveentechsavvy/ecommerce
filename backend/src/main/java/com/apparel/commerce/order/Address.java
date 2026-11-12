package com.apparel.commerce.order;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Address snapshot stored on the order (immutable history even if the customer edits their address book). */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    @NotBlank @Size(max = 120) private String fullName;
    @NotBlank @Pattern(regexp = "^[0-9+ -]{8,20}$", message = "must be a valid phone number") private String phone;
    @NotBlank @Size(max = 200) private String line1;
    @Size(max = 200) private String line2;
    @NotBlank @Size(max = 80) private String city;
    @NotBlank @Size(max = 80) private String state;
    @NotBlank @Pattern(regexp = "^[0-9A-Za-z -]{3,12}$", message = "must be a valid postal code") private String postalCode;
    @NotBlank @Size(min = 2, max = 2) private String country;
}
