package com.apparel.commerce.content;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface BannerRepository extends JpaRepository<Banner, Long> {

    @Query("""
        select b from Banner b
        where b.placement = :placement and b.active = true
          and (b.startsAt is null or b.startsAt <= :now)
          and (b.endsAt is null or b.endsAt > :now)
        order by b.sortOrder, b.id
        """)
    List<Banner> findLive(@Param("placement") Banner.Placement placement, @Param("now") Instant now);

    List<Banner> findAllByOrderByPlacementAscSortOrderAsc();
}

interface HomeSectionRepository extends JpaRepository<HomeSection, Long> {
    List<HomeSection> findAllByOrderBySortOrderAsc();

    List<HomeSection> findByActiveTrueOrderBySortOrderAsc();
}

interface NavigationItemRepository extends JpaRepository<NavigationItem, Long> {
    List<NavigationItem> findAllByOrderBySortOrderAscIdAsc();

    boolean existsByParentId(Long parentId);
}
