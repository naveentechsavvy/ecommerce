package com.apparel.commerce.content;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/** An ordered block on the homepage; its {@code config} drives what the storefront renders. */
@Entity
@Table(name = "home_section")
@Getter
@Setter
public class HomeSection {

    public enum Type { HERO_CAROUSEL, PRODUCT_RAIL, CATEGORY_GRID, BANNER_STRIP, RICH_TEXT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Type sectionType;

    private String title;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> config = new HashMap<>();

    private int sortOrder;
    private boolean active = true;

    @UpdateTimestamp
    private Instant updatedAt;
}
