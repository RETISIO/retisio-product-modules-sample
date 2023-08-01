package com.retisio.arc.service;

import akka.Done;
import akka.actor.ActorSystem;
import akka.actor.typed.javadsl.Adapter;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.EntityRef;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.retisio.arc.aggregate.catalog.Catalog;
import com.retisio.arc.aggregate.catalog.CatalogAggregate;
import com.retisio.arc.aggregate.catalog.CatalogCommand;
import com.retisio.arc.aggregate.catalog.CatalogEvent;
import com.retisio.arc.csv.model.CsvCatalog;
import com.retisio.arc.exception.AlreadyExistsException;
import com.retisio.arc.exception.Error;
import com.retisio.arc.exception.IllegalOperationException;
import com.retisio.arc.exception.NotFoundException;
import com.retisio.arc.execution.ServiceExecutionContext;
import com.retisio.arc.listener.MessageListener;
import com.retisio.arc.listener.handler.BrandMessageHandler;
import com.retisio.arc.projection.ReadSideProjection;
import com.retisio.arc.projection.handler.CatalogProjectionHandler;
import com.retisio.arc.repository.catalog.CatalogRepository;
import com.retisio.arc.request.catalog.CreateCatalogRequest;
import com.retisio.arc.request.catalog.PatchCatalogRequest;
import com.retisio.arc.request.catalog.UpdateCatalogRequest;
import com.retisio.arc.response.catalog.GetCatalogResponse;
import com.retisio.arc.response.catalog.GetCatalogsResponse;
import com.retisio.arc.util.CsvUtil;
import com.retisio.arc.util.KafkaUtil;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Slf4j
public class CatalogServiceImpl implements CatalogService {

    private final ClusterSharding clusterSharding;
    private final ActorSystem classicActorSystem;

    @Inject
    private ServiceExecutionContext serviceExecutionContext;

    @Inject
    private CatalogRepository catalogRepository;

    @Inject
    public CatalogServiceImpl(ActorSystem classicActorSystem,
                              BrandMessageHandler brandMessageHandler,
                              KafkaUtil kafkaUtil){
        this.classicActorSystem = classicActorSystem;
        akka.actor.typed.ActorSystem<Void> typedActorSystem = Adapter.toTyped(classicActorSystem);
        this.clusterSharding = ClusterSharding.get(typedActorSystem);
        String entityType = CatalogAggregate.ENTITY_TYPE_KEY.name();
        //-------------------
        Optional.ofNullable(
                typedActorSystem.settings().config().getStringList("retisio.aggregates.enabled")
        )
                .map(list -> {
                    list.stream().forEach(a -> {
                        if(a.equals("CatalogAggregate")){
                            CatalogAggregate.init(clusterSharding,3,35);
                        }
                    });
                    return Done.getInstance();
                });
        //------------------------
        Optional.ofNullable(
                typedActorSystem.settings().config().getConfigList("retisio.projections.enabled")
        ).map(list -> {
            list.stream().forEach(c -> {
                Integer count = c.getInt("count");
                String name = c.getString("name");
                String key = c.getString("key");
                String topic = c.getString("topic");
                if(c.getString("handler").equals("CatalogProjectionHandler")){
                    CatalogProjectionHandler catalogProjectionHandler = new CatalogProjectionHandler(clusterSharding, topic, kafkaUtil);
                    ReadSideProjection.<CatalogEvent>init(count, typedActorSystem, entityType, name, key, catalogProjectionHandler);
                }
            });
            return Done.getInstance();
        });
        //-------------------------
        Optional.ofNullable(
                typedActorSystem.settings().config().getConfigList("retisio.listeners.enabled")
        ).map(list -> {
            list.stream().forEach(c -> {
                Integer count = c.getInt("count");
                String topic = c.getString("topic");
                String group = c.getString("group");
                if(c.getString("handler").equals("BrandMessageHandler")){
                    MessageListener.init(count, typedActorSystem,topic,group, brandMessageHandler);
                }
            });
            return Done.getInstance();
        });

    }

    private static final Duration askTimeout = Duration.ofSeconds(10);

    private EntityRef<CatalogCommand> ref(String entityId) {
        return clusterSharding.entityRefFor(CatalogAggregate.ENTITY_TYPE_KEY, entityId);
    }

    private CompletionStage<Optional<Catalog>> getCatalog(EntityRef<CatalogCommand> ref) {
        return ref.ask(replyTo -> new CatalogCommand.GetCatalog(ref.getEntityId(), replyTo), askTimeout);
    }

    private CompletionStage<Optional<Catalog>> createCatalog(CreateCatalogRequest request, EntityRef<CatalogCommand> ref) {
        return ref.<Done>ask(replyTo -> new CatalogCommand.CreateCatalog(
                            request.getCatalogId(),
                            request.getCatalogName(),
                            request.getActive(),
                            request.getDynProps(),
                            replyTo
                    ), askTimeout)
                .thenCompose(done -> getCatalog(ref));
    }
    private CompletionStage<Optional<Catalog>> updateCatalog(UpdateCatalogRequest request, EntityRef<CatalogCommand> ref) {
        return ref.<Done>ask(replyTo -> new CatalogCommand.UpdateCatalog(
                            request.getCatalogId(),
                            request.getCatalogName(),
                            request.getActive(),
                            request.getDynProps(),
                            replyTo
                    ), askTimeout)
                .thenCompose(done -> getCatalog(ref));
    }
    private CompletionStage<Optional<Catalog>> patchCatalog(String id, PatchCatalogRequest request, EntityRef<CatalogCommand> ref) {
        return ref.<Done>ask(replyTo -> new CatalogCommand.PatchCatalog(
                            id,
                            request.getCatalogName(),
                            request.getActive(),
                            request.getDynProps(),
                            replyTo
                    ), askTimeout)
                .thenCompose(done -> getCatalog(ref));
    }
    private CompletionStage<Optional<Catalog>> deleteCatalog(String id, EntityRef<CatalogCommand> ref) {
        return ref.<Done>ask(replyTo -> new CatalogCommand.DeleteCatalog(
                            id,
                            replyTo
                    ), askTimeout)
                .thenCompose(done -> getCatalog(ref));
    }

    @Override
    public CompletionStage<GetCatalogResponse> createCatalog(CreateCatalogRequest request) {
        return getCatalog(ref(request.getCatalogId()))
                .thenCompose(optionalCatalog -> {
                    if(optionalCatalog.isPresent()){
                        if(optionalCatalog.get().getDeleted().booleanValue() == true){
                            throw new IllegalOperationException(
                                    Arrays.asList(
                                            new Error("ERROR_003", "Create on already deleted catalog "+request.getCatalogId()+" is not allowed.")
                                    )
                            );
                        }
                        throw new AlreadyExistsException(
                                Arrays.asList(
                                        new Error("ERROR_001", "Catalog "+request.getCatalogId()+" is already exists.")
                                )
                        );
                    }
                    return createCatalog(request, ref(request.getCatalogId()))
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
                });

    }

    @Override
    public CompletionStage<GetCatalogResponse> updateCatalog(UpdateCatalogRequest request) {
        return getCatalog(ref(request.getCatalogId()))
                .thenCompose(optionalCatalog -> {
                    if(optionalCatalog.isPresent()){
                        if(optionalCatalog.get().getDeleted().booleanValue() == true){
                            throw new IllegalOperationException(
                                    Arrays.asList(
                                            new Error("ERROR_003", "Update on already deleted catalog "+request.getCatalogId()+" is not allowed.")
                                    )
                            );
                        }
                    }
                    return updateCatalog(request, ref(request.getCatalogId()))
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
                });
    }

    @Override
    public CompletionStage<GetCatalogResponse> patchCatalog(PatchCatalogRequest request, String id) {
        return getCatalog(ref(id))
                .thenCompose(optionalCatalog -> {
                    if(!optionalCatalog.isPresent()){
                        throw new NotFoundException(
                                Arrays.asList(
                                        new Error("ERROR_002", "Catalog "+id+" is not found.")
                                )
                        );
                    } else if(optionalCatalog.get().getDeleted().booleanValue() == true){
                        throw new IllegalOperationException(
                                Arrays.asList(
                                        new Error("ERROR_003", "Patch on already deleted catalog "+id+" is not allowed.")
                                )
                        );
                    }
                    return patchCatalog(id, request, ref(id))
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
                });
    }

    @Override
    public CompletionStage<GetCatalogResponse> deleteCatalog(String id) {
        return getCatalog(ref(id))
                .thenCompose(optionalCatalog -> {
                    if(!optionalCatalog.isPresent()){
                        throw new NotFoundException(
                                Arrays.asList(
                                        new Error("ERROR_002", "Catalog "+id+" is not found.")
                                )
                        );
                    } else if(optionalCatalog.get().getDeleted().booleanValue() == true){
                        throw new IllegalOperationException(
                                Arrays.asList(
                                        new Error("ERROR_003", "Delete on catalog "+id+" is not allowed.")
                                )
                        );
                    }
                    return deleteCatalog(id, ref(id))
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
                });
    }

    @Override
    public CompletionStage<List<String>> importCsvCatalog(List<CsvCatalog> csvCatalogs) {
        return Source.from(csvCatalogs)
                .mapAsync(1, record -> {
                    UpdateCatalogRequest updateCatalogRequest = new UpdateCatalogRequest(
                            record.getCatalogId(),
                            record.getCatalogName(),
                            record.getActive(),
                            null
                    );
                    return updateCatalog(updateCatalogRequest)
                            .thenApply(r -> {
                                log.info("Catalog {} is imported.", r.getCatalogId());
                                return r;
                            })
                            .thenApply(b -> Optional.ofNullable(record.getCatalogId()));
                })
                .toMat(Sink.<List<String>, Optional<String>>fold(new ArrayList<>(), (aggr, next) -> {
                    if(next.isPresent())
                        aggr.add(next.get());
                    return aggr;
                }), Keep.right()).run(classicActorSystem);
    }

    @Override
    public CompletionStage<byte[]> exportCsvCatalog(String fileName) {
        return catalogRepository.getCatalogs(Optional.empty(), Optional.of("100"), Optional.of("0"))
                .thenApply(r -> r.getCatalogs())
                .thenCompose(list -> prepareCsvCatalogList(list))
                .thenApply(list -> {
                    String tempFolder = "c:/tmp/";
                    List<String> columns = Arrays.asList(
                            "Catalog Id",
                            "Catalog Name",
                            "Active"
                    );
                    return CsvUtil.getBytes(list, tempFolder, fileName, columns, CsvCatalog.class);
                });

    }

    private CompletionStage<List<CsvCatalog>> prepareCsvCatalogList(List<GetCatalogResponse> list){
        return Source.from(list)
                .mapAsync(1, record -> {
                    return CompletableFuture.supplyAsync(()->{
                        CsvCatalog csvCatalog = new CsvCatalog();
                        csvCatalog.setCatalogId(record.getCatalogId());
                        csvCatalog.setCatalogName(record.getCatalogName());
                        csvCatalog.setActive(record.getActive());
                        return Optional.of(csvCatalog);
                    });
                })
                .toMat(Sink.<List<CsvCatalog>, Optional<CsvCatalog>>fold(new ArrayList<>(), (aggr, next) -> {
                    if(next.isPresent())
                        aggr.add(next.get());
                    return aggr;
                }), Keep.right()).run(classicActorSystem);
    }

    @Override
    public CompletionStage<GetCatalogsResponse> getCatalogs(Optional<String> filter, Optional<String> limit, Optional<String> offset) {
        return catalogRepository.getCatalogs(filter, limit, offset);
    }

    @Override
    public CompletionStage<GetCatalogResponse> getCatalog(String id) {
        return getCatalog(ref(id))
                .thenCompose(optionalCatalog -> {
                    if(!optionalCatalog.isPresent()){
                        throw new NotFoundException(
                                Arrays.asList(
                                        new Error("ERROR_002", "Catalog "+id+" is not found.")
                                )
                        );
                    } else if(optionalCatalog.get().getDeleted().booleanValue() == true){
                        throw new IllegalOperationException(
                                Arrays.asList(
                                        new Error("ERROR_003", "Catalog "+id+" is deleted.")
                                )
                        );
                    }
                    Catalog catalog = optionalCatalog.get();
                    return CompletableFuture.completedFuture(
                            GetCatalogResponse.builder()
                            .catalogId(catalog.getCatalogId())
                            .catalogName(catalog.getCatalogName())
                            .active(catalog.getActive())
                            .dynProps(catalog.getDynProps())
                            .deleted(catalog.getDeleted())
                            .build()
                    );
                });
    }

}
