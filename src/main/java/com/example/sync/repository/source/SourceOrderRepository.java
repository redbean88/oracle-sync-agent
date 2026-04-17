package com.example.sync.repository.source;

import com.example.sync.entity.source.SourceOrder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SourceOrderRepository extends JpaRepository<SourceOrder, Long> {

    @Query("SELECT o FROM SourceOrder o WHERE o.id > :lastId ORDER BY o.id ASC")
    List<SourceOrder> findByLastIdWithLimit(@Param("lastId") Long lastId, Pageable pageable);
}
