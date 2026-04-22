package com.example.sync.domain.source;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Orders {

    @Id
    private Long id;

    @Column(name = "order_no", length = 50)
    private String orderNo;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "amount", precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Orders() {}

    public Long getId() { return id; }

    public String getOrderNo() { return orderNo; }

    public Long getCustomerId() { return customerId; }

    public String getStatus() { return status; }

    public BigDecimal getAmount() { return amount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
