package com.apparel.commerce.content;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "banner")
@Getter
@Setter
public class Banner {

    public enum Placement { HOME_HERO, HOME_STRIP, PLP_TOP, PDP_PROMO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String subtitle;
    private String imageUrl;
    private String mobileImageUrl;
    private String ctaText;
    private String ctaLink;

    @Enumerated(EnumType.STRING)
    private Placement placement;

    private int sortOrder;
    private boolean active = true;
    private Instant startsAt;
    private Instant endsAt;

    @CreationTimestamp
    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;
}
