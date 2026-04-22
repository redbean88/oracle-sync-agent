package com.example.sync.repository.source;

import com.example.sync.domain.source.Orders;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SourceOrderRepository extends JpaRepository<Orders, Long> {

    @Query("SELECT o FROM Orders o WHERE o.id > :lastId ORDER BY o.id ASC")
    List<Orders> findByLastIdWithLimit(@Param("lastId") Long lastId, Pageable pageable);
}
