package com.example.sync.domain.source;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;


/**
 * QSourceOrder is a Querydsl query type for SourceOrder
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSourceOrder extends EntityPathBase<SourceOrder> {

    private static final long serialVersionUID = 1341912168L;

    public static final QSourceOrder sourceOrder = new QSourceOrder("sourceOrder");

    public final NumberPath<java.math.BigDecimal> amount = createNumber("amount", java.math.BigDecimal.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> customerId = createNumber("customerId", Long.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath orderNo = createString("orderNo");

    public final StringPath status = createString("status");

    public QSourceOrder(String variable) {
        super(SourceOrder.class, forVariable(variable));
    }

    public QSourceOrder(Path<? extends SourceOrder> path) {
        super(path.getType(), path.getMetadata());
    }

    public QSourceOrder(PathMetadata metadata) {
        super(SourceOrder.class, metadata);
    }

}

