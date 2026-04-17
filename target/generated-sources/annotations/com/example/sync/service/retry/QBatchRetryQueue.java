package com.example.sync.service.retry;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;


/**
 * QBatchRetryQueue is a Querydsl query type for BatchRetryQueue
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBatchRetryQueue extends EntityPathBase<BatchRetryQueue> {

    private static final long serialVersionUID = 823074242L;

    public static final QBatchRetryQueue batchRetryQueue = new QBatchRetryQueue("batchRetryQueue");

    public final StringPath errorMessage = createString("errorMessage");

    public final StringPath errorType = createString("errorType");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> maxRetry = createNumber("maxRetry", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> nextRetryAt = createDateTime("nextRetryAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> retryCount = createNumber("retryCount", Integer.class);

    public final StringPath sourceIds = createString("sourceIds");

    public final StringPath status = createString("status");

    public QBatchRetryQueue(String variable) {
        super(BatchRetryQueue.class, forVariable(variable));
    }

    public QBatchRetryQueue(Path<? extends BatchRetryQueue> path) {
        super(path.getType(), path.getMetadata());
    }

    public QBatchRetryQueue(PathMetadata metadata) {
        super(BatchRetryQueue.class, metadata);
    }

}

