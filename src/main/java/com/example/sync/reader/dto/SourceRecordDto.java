package com.example.sync.reader.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SourceRecordDto {
    private Long id;
    private String orderNo;
    private Long customerId;
    private String status;
    private BigDecimal amount;
    private LocalDateTime createdAt;
}
