package com.example.sync.domain.proxy;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;


/**
 * QBatchRetryQueueEntity is a Querydsl query type for BatchRetryQueueEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBatchRetryQueueEntity extends EntityPathBase<BatchRetryQueueEntity> {

    private static final long serialVersionUID = -419301538L;

    public static final QBatchRetryQueueEntity batchRetryQueueEntity = new QBatchRetryQueueEntity("batchRetryQueueEntity");

    public final StringPath errorMessage = createString("errorMessage");

    public final StringPath errorType = createString("errorType");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> maxRetry = createNumber("maxRetry", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> nextRetryAt = createDateTime("nextRetryAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> retryCount = createNumber("retryCount", Integer.class);

    public final StringPath sourceIds = createString("sourceIds");

    public final StringPath status = createString("status");

    public QBatchRetryQueueEntity(String variable) {
        super(BatchRetryQueueEntity.class, forVariable(variable));
    }

    public QBatchRetryQueueEntity(Path<? extends BatchRetryQueueEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QBatchRetryQueueEntity(PathMetadata metadata) {
        super(BatchRetryQueueEntity.class, metadata);
    }

}

