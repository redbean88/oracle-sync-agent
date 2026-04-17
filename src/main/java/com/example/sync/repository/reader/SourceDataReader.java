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
    public List<SourceRecordDto> readChunk(long lastId, int chunkSize) {
        return repository.findByLastIdWithLimit(lastId, PageRequest.of(0, chunkSize))
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /** 재시도 복구를 위한 ID 기반 데이터 조회 */
    @Transactional(readOnly = true, transactionManager = "sourceTransactionManager")
    public List<SourceRecordDto> fetchByIds(List<Long> ids) {
        return repository.findAllById(ids)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private SourceRecordDto convertToDto(com.example.sync.domain.source.SourceOrder order) {
        return new SourceRecordDto(
                order.getId(),
                order.getOrderNo(),
                order.getCustomerId(),
                order.getStatus(),
                order.getAmount(),
                order.getCreatedAt()
        );
    }
}
