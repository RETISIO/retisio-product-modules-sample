package com.retisio.arc.ext.service;

import akka.Done;
import akka.actor.ActorSystem;
import akka.actor.typed.javadsl.Adapter;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.EntityRef;
import com.retisio.arc.aggregate.catalog.Catalog;
import com.retisio.arc.aggregate.catalog.CatalogAggregate;
import com.retisio.arc.aggregate.catalog.CatalogCommand;
import com.retisio.arc.aggregate.catalog.CatalogEvent;
import com.retisio.arc.exception.Error;
import com.retisio.arc.exception.IllegalOperationException;
import com.retisio.arc.execution.ServiceExecutionContext;
import com.retisio.arc.ext.aggregate.catalog.ExtCatalogAggregate;
import com.retisio.arc.ext.aggregate.catalog.ExtCatalogCommand;
import com.retisio.arc.ext.listener.handler.ExtBrandMessageHandler;
import com.retisio.arc.ext.projection.handler.ExtCatalogProjectionHandler;
import com.retisio.arc.listener.MessageListener;
import com.retisio.arc.projection.ReadSideProjection;
import com.retisio.arc.projection.handler.CatalogProjectionHandler;
import com.retisio.arc.response.catalog.GetCatalogResponse;
import com.retisio.arc.service.CatalogService;
import com.retisio.arc.util.KafkaUtil;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Slf4j
public class ExtCatalogServiceImpl implements ExtCatalogService {

    private final ClusterSharding clusterSharding;

    @Inject
    private ServiceExecutionContext serviceExecutionContext;

    @Inject
    public ExtCatalogServiceImpl(ActorSystem classicActorSystem, KafkaUtil kafkaUtil,
                                 ExtBrandMessageHandler extBrandMessageHandler){
        akka.actor.typed.ActorSystem<Void> typedActorSystem = Adapter.toTyped(classicActorSystem);
        this.clusterSharding = ClusterSharding.get(typedActorSystem);
        String entityType = CatalogAggregate.ENTITY_TYPE_KEY.name();
        //---------------------------
        Optional.ofNullable(
                typedActorSystem.settings().config().getStringList("retisio.aggregates.enabled")
        )
                .map(list -> {
                    list.stream().forEach(a -> {
                        if(a.equals("ExtCatalogAggregate")){
                            log.info("ExtCatalogAggregate ***************************************");
                            ExtCatalogAggregate.init(clusterSharding,3,35);
                        }
                    });
                    return Done.getInstance();
                });
        //--------------------------------
        Optional.ofNullable(
                typedActorSystem.settings().config().getConfigList("retisio.projections.enabled")
        ).map(list -> {
            list.stream().forEach(c -> {
                Integer count = c.getInt("count");
                String name = c.getString("name");
                String key = c.getString("key");
                String topic = c.getString("topic");
                if(c.getString("handler").equals("ExtCatalogProjectionHandler")){
                    ExtCatalogProjectionHandler extCatalogProjectionHandler = new ExtCatalogProjectionHandler(clusterSharding, topic, kafkaUtil);
                    ReadSideProjection.<CatalogEvent>init(count, typedActorSystem, entityType, name, key, extCatalogProjectionHandler);
                }
            });
            return Done.getInstance();
        });
        //----------------------------
        //-------------------------
        Optional.ofNullable(
                typedActorSystem.settings().config().getConfigList("retisio.listeners.enabled")
        ).map(list -> {
            list.stream().forEach(c -> {
                Integer count = c.getInt("count");
                String topic = c.getString("topic");
                String group = c.getString("group");
                if(c.getString("handler").equals("ExtBrandMessageHandler")){
                    MessageListener.init(count, typedActorSystem,topic,group, extBrandMessageHandler);
                }
            });
            return Done.getInstance();
        });
    }

    private static final Duration askTimeout = Duration.ofSeconds(10);

    public EntityRef<CatalogCommand> ref(String entityId) {
        return clusterSharding.entityRefFor(CatalogAggregate.ENTITY_TYPE_KEY, entityId);
    }

    public CompletionStage<Optional<Catalog>> getCatalog(EntityRef<CatalogCommand> ref) {
        return ref.ask(reply -> new CatalogCommand.GetCatalog(ref.getEntityId(), reply), askTimeout);
    }

    public CompletionStage<Optional<Catalog>> activateCatalog(EntityRef<CatalogCommand> ref) {
        return ref.<Done>ask(replyTo -> new ExtCatalogCommand.ActivateCatalog(ref.getEntityId(), replyTo), askTimeout)
                .thenCompose(done -> getCatalog(ref));
    }

    @Override
    public CompletionStage<GetCatalogResponse> deleteCatalog(String catalogId) {
        return CompletableFuture.supplyAsync(()->{
            throw new IllegalOperationException(
                    Arrays.asList(
                            new Error("NOT_ALLOED", "Catalog cann't be deleted. Use active flag to inactive the Catalog")
                    )
            );
        });
    }

    @Override
    public CompletionStage<GetCatalogResponse> activateCatalog(String catalogId) {
        return activateCatalog(ref(catalogId))
                .thenApply(optCatalog -> {
                    if(optCatalog.isPresent()){
                        Catalog catalog = optCatalog.get();
                        return GetCatalogResponse.builder()
                                .catalogId(catalog.getCatalogId())
                                .catalogName(catalog.getCatalogName())
                                .active(catalog.getActive())
                                .dynProps(catalog.getDynProps())
                                .deleted(catalog.getDeleted())
                                .build();
                    }
                    return GetCatalogResponse.builder().build();
                });
    }
}
