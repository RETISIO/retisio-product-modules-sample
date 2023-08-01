package com.retisio.arc.ext.projection.handler;

import akka.Done;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.projection.r2dbc.javadsl.R2dbcSession;
import com.retisio.arc.aggregate.catalog.Catalog;
import com.retisio.arc.aggregate.catalog.CatalogEvent;
import com.retisio.arc.ext.aggregate.catalog.ExtCatalogEvent;
import com.retisio.arc.ext.message.catalog.ExtCatalogMessage;
import com.retisio.arc.message.catalog.CatalogMessage;
import com.retisio.arc.projection.handler.CatalogProjectionHandler;
import com.retisio.arc.r2dbc.StatementWrapper;
import com.retisio.arc.util.KafkaUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ExtCatalogProjectionHandler extends CatalogProjectionHandler {
    public ExtCatalogProjectionHandler(ClusterSharding clusterSharding, String topic, KafkaUtil kafkaUtil) {
        super(clusterSharding, topic, kafkaUtil);
    }

    @Override
    protected CompletionStage<Done> processReadSideExt(R2dbcSession session, CatalogEvent event) {
        if(event instanceof ExtCatalogEvent.CatalogActivated) {
            return activateCatalog(session, (ExtCatalogEvent.CatalogActivated)event);
        }else {
            return CompletableFuture.completedFuture(Done.getInstance());
        }
    }
    private CompletionStage<Done> activateCatalog(R2dbcSession session, ExtCatalogEvent.CatalogActivated event) {
        log.info("activateCatalog catalogId::{}", event.catalogId);
        AtomicInteger index = new AtomicInteger(-1);
        String query = "update CATALOG set is_active = true, LAST_MODIFIED_TMST=now() where catalog_id = $1";
        log.info("query::{}", query);
        StatementWrapper statementWrapper = new StatementWrapper(session.createStatement(query));
        statementWrapper.bind(index.incrementAndGet(), event.catalogId, String.class);
        return session.updateOne(statementWrapper.getStatement())
                .thenApply(rowsUpdated -> Done.getInstance());
    }
    @Override
    protected boolean isExtEvent(CatalogEvent event) {
        return (event instanceof ExtCatalogEvent.CatalogActivated);
    }

    @Override
    protected CatalogMessage convertToCatalogPublishEventExt(CatalogEvent event, Catalog catalog) {
        if(event instanceof ExtCatalogEvent.CatalogActivated){
            return new ExtCatalogMessage.CatalogActivatedMessage(convertToCatalogMessage(catalog));
        } else {
            log.error("Try to convert non publish CatalogEvent: {}", event);
            throw new IllegalArgumentException("non publish CatalogEvent");
        }
    }
}
