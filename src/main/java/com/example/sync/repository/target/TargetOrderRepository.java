package com.example.sync.repository.target;

import com.example.sync.domain.target.TargetOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TargetOrderRepository extends JpaRepository<TargetOrder, Long> {
}
