package com.nethcare.repository;

import com.nethcare.model.StockCategory;
import com.nethcare.model.StockItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface StockItemRepository extends JpaRepository<StockItem, Long> {

    Optional<StockItem> findByItemCode(String itemCode);

    List<StockItem> findByCategoryOrderByNameAsc(StockCategory category);

    List<StockItem> findByIsActiveTrueOrderByCategoryAscNameAsc();

    /**
     * Anything whose free stock has fallen to its reorder level or below.
     *
     * Written as a query because "quantity minus reserved" is a calculation
     * JPA cannot express in a derived method name. An item with quantity 5 and
     * 3 reserved has 2 free, so it is low if reorderLevel is 2 or more.
     */
    @Query("""
           select s from StockItem s
           where s.isActive = true
             and (s.quantity - s.reserved) <= s.reorderLevel
           order by s.category asc, s.name asc
           """)
    List<StockItem> findLowStock();

    /** Gallery search — the public page has no login, so it filters on listing. */
    @Query("""
           select s from StockItem s
           where s.isActive = true
             and (:category is null or s.category = :category)
             and (:q is null
                  or lower(s.name) like lower(concat('%', :q, '%'))
                  or lower(s.brand) like lower(concat('%', :q, '%'))
                  or lower(s.itemCode) like lower(concat('%', :q, '%')))
           order by s.category asc, s.name asc
           """)
    List<StockItem> searchListed(StockCategory category, String q);

    List<StockItem> findByUnitPriceGreaterThan(BigDecimal price);
}
