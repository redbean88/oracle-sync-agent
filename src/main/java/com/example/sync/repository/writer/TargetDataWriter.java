package com.example.sync.repository.writer;

import com.example.sync.infra.exception.TransientSyncException;
import com.example.sync.domain.dto.SourceRecordDto;
import com.example.sync.entity.target.TargetOrder;
import com.example.sync.repository.target.TargetOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TargetDataWriter {

    private static final Logger log = LoggerFactory.getLogger(TargetDataWriter.class);

    private final TargetOrderRepository repository;

    public TargetDataWriter(TargetOrderRepository repository) {
        this.repository = repository;
    }

    @Retryable(
        include = {TransientSyncException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @Transactional(transactionManager = "targetTransactionManager")
    public void bulkUpsert(List<SourceRecordDto> records) {
        List<TargetOrder> targetOrders = records.stream().map(r -> {
            TargetOrder t = new TargetOrder();
            t.setId(r.getId());
            t.setOrderNo(r.getOrderNo());
            t.setCustomerId(r.getCustomerId());
            t.setStatus(r.getStatus());
            t.setAmount(r.getAmount());
            t.setCreatedAt(r.getCreatedAt());
            return t;
        }).collect(Collectors.toList());
        
        repository.saveAll(targetOrders);
    }

    @Recover
    public void recover(TransientSyncException e, List<SourceRecordDto> records) {
        throw e;
    }
}
