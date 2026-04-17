package com.example.sync.repository.reader;

import com.example.sync.dto.SourceRecordDto;
import com.example.sync.repository.source.SourceOrderRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SourceDataReader {

    private final SourceOrderRepository repository;

    public SourceDataReader(SourceOrderRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true, transactionManager = "sourceTransactionManager")
    public List<SourceRecordDto> fetchChunk(long lastId, int chunkSize) {
        return repository.findByLastIdWithLimit(lastId, PageRequest.of(0, chunkSize))
                .stream()
                .map(order -> new SourceRecordDto(
                        order.getId(),
                        order.getOrderNo(),
                        order.getCustomerId(),
                        order.getStatus(),
                        order.getAmount(),
                        order.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }
}
