package com.retisio.arc.ext.controller;

import akka.actor.ActorSystem;
import akka.actor.typed.javadsl.Adapter;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import com.retisio.arc.controller.CatalogServiceController;
import com.retisio.arc.ext.aggregate.catalog.ExtCatalogAggregate;
import com.retisio.arc.ext.response.catalog.ExtGetCatalogResponse;
import com.retisio.arc.ext.service.ExtCatalogService;
import com.retisio.arc.service.CatalogService;
import lombok.extern.slf4j.Slf4j;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Slf4j
public class ExtCatalogServiceController extends Controller {

    @Inject
    private CatalogService catalogService;

    @Inject
    private ExtCatalogService extCatalogService;

    public CompletionStage<Result> ping() {
        log.info(" ..... ping");
        return CompletableFuture.completedFuture(ok("Ext Ping - Ok"));
    }
    public CompletionStage<Result> deleteCatalog(String catalogId) {
        log.info("deleteCatalog api invoked ....");
        return extCatalogService.deleteCatalog(catalogId)
                .thenApply(r -> ok(Json.toJson(r)));
    }

    //overriden
    public CompletionStage<Result> activateCatalog(String catalogId) {
        log.info("activateCatalog api invoked ....");
        return extCatalogService.activateCatalog(catalogId)
                .thenApply(r -> ExtGetCatalogResponse.builder()
                        .catalogId(r.getCatalogId())
                        .catalogName(r.getCatalogName())
                        .active(r.getActive())
                        .dynProps(r.getDynProps())
                        .build()
                )
                .thenApply(r -> ok(Json.toJson(r)));
    }
}