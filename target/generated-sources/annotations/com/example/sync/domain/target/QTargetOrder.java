package com.example.sync.domain.target;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;


/**
 * QTargetOrder is a Querydsl query type for TargetOrder
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTargetOrder extends EntityPathBase<TargetOrder> {

    private static final long serialVersionUID = 1884613928L;

    public static final QTargetOrder targetOrder = new QTargetOrder("targetOrder");

    public final NumberPath<java.math.BigDecimal> amount = createNumber("amount", java.math.BigDecimal.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> customerId = createNumber("customerId", Long.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath orderNo = createString("orderNo");

    public final StringPath status = createString("status");

    public QTargetOrder(String variable) {
        super(TargetOrder.class, forVariable(variable));
    }

    public QTargetOrder(Path<? extends TargetOrder> path) {
        super(path.getType(), path.getMetadata());
    }

    public QTargetOrder(PathMetadata metadata) {
        super(TargetOrder.class, metadata);
    }

}

