package com.example.sync.domain.proxy;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;


/**
 * QSyncCheckpoint is a Querydsl query type for SyncCheckpoint
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSyncCheckpoint extends EntityPathBase<SyncCheckpoint> {

    private static final long serialVersionUID = -13839861L;

    public static final QSyncCheckpoint syncCheckpoint = new QSyncCheckpoint("syncCheckpoint");

    public final StringPath jobName = createString("jobName");

    public final NumberPath<Long> lastId = createNumber("lastId", Long.class);

    public final DateTimePath<java.time.LocalDateTime> lastSyncedAt = createDateTime("lastSyncedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> processedCnt = createNumber("processedCnt", Long.class);

    public QSyncCheckpoint(String variable) {
        super(SyncCheckpoint.class, forVariable(variable));
    }

    public QSyncCheckpoint(Path<? extends SyncCheckpoint> path) {
        super(path.getType(), path.getMetadata());
    }

    public QSyncCheckpoint(PathMetadata metadata) {
        super(SyncCheckpoint.class, metadata);
    }

}

