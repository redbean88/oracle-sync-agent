package com.example.sync.reader.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SourceRecordDto {
    private Long id;
    private String orderNo;
    private Long customerId;
    private String status;
    private BigDecimal amount;
    private LocalDateTime createdAt;

    public SourceRecordDto() {}

    public SourceRecordDto(Long id, String orderNo, Long customerId, String status, BigDecimal amount, LocalDateTime createdAt) {
        this.id = id;
        this.orderNo = orderNo;
        this.customerId = customerId;
        this.status = status;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getOrderNo() { return orderNo; }
    public Long getCustomerId() { return customerId; }
    public String getStatus() { return status; }
    public BigDecimal getAmount() { return amount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
