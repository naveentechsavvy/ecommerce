package com.apparel.commerce.catalog;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "product")
@Getter
@Setter
public class Product {

    public enum Status { DRAFT, ACTIVE, ARCHIVED }

    public enum Gender { MEN, WOMEN, UNISEX, KIDS }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "size_chart_id")
    private SizeChart sizeChart;

    private String name;
    private String slug;
    private String brand;
    private String description;
    private String material;
    private String careInstructions;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private BigDecimal basePrice;
    private BigDecimal compareAtPrice;

    @Enumerated(EnumType.STRING)
    private Status status = Status.DRAFT;

    private int popularityScore;
    private String marketingBadge;
    private String marketingHtml;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder")
    private List<ProductImage> images = new ArrayList<>();

    @CreationTimestamp
    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;

    /** Sellable units across all active variants. */
    public int totalAvailable() {
        return variants.stream().filter(ProductVariant::isActive).mapToInt(ProductVariant::available).sum();
    }

    public String primaryImageUrl() {
        return images.isEmpty() ? null : images.get(0).getUrl();
    }
}
