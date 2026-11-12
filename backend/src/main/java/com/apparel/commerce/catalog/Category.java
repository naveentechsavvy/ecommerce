package com.apparel.commerce.catalog;

import com.apparel.commerce.common.Visibility;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "category")
@Getter
@Setter
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private int sortOrder;

    @Enumerated(EnumType.STRING)
    private Visibility visibility = Visibility.ENABLED;

    @CreationTimestamp
    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;

    public Long getParentId() {
        return parent == null ? null : parent.getId();
    }
}
