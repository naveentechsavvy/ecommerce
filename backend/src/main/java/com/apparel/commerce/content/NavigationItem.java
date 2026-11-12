package com.apparel.commerce.content;

import com.apparel.commerce.catalog.Category;
import com.apparel.commerce.common.Visibility;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Mega-menu entry. Top-level items are columns; children are links inside the dropdown. */
@Entity
@Table(name = "navigation_item")
@Getter
@Setter
public class NavigationItem {

    public enum LinkType { CATEGORY, URL, NONE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_id")
    private Long parentId;

    private String label;

    @Enumerated(EnumType.STRING)
    private LinkType linkType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    private String url;
    private String badgeText;
    private int sortOrder;

    @Enumerated(EnumType.STRING)
    private Visibility visibility = Visibility.ENABLED;
}
