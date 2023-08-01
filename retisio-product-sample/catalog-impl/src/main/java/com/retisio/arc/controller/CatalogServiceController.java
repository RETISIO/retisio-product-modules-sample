package com.retisio.arc.controller;

import akka.actor.ActorSystem;
import akka.actor.typed.javadsl.Adapter;
import akka.management.cluster.bootstrap.ClusterBootstrap;
import akka.management.javadsl.AkkaManagement;
import com.retisio.arc.csv.model.CsvCatalog;
import com.retisio.arc.request.catalog.CreateCatalogRequest;
import com.retisio.arc.request.catalog.PatchCatalogRequest;
import com.retisio.arc.request.catalog.UpdateCatalogRequest;
import com.retisio.arc.service.CatalogService;
import com.retisio.arc.util.CsvUtil;
import lombok.extern.slf4j.Slf4j;
import play.libs.Files;
import play.libs.Json;
import play.mvc.*;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Slf4j
public class CatalogServiceController extends Controller {

    private final ActorSystem classicActorSystem;

    @Inject
    private CatalogService catalogService;

    @Inject
    public CatalogServiceController(ActorSystem classicActorSystem){
        this.classicActorSystem = classicActorSystem;

        akka.actor.typed.ActorSystem<Void> typedActorSystem = Adapter.toTyped(classicActorSystem);

        AkkaManagement.get(typedActorSystem).start();
        ClusterBootstrap.get(typedActorSystem).start();
    }

    public CompletionStage<Result> ping() {
        return CompletableFuture.completedFuture(ok("Ok"));
    }


    public CompletionStage<Result> getCatalogs(Http.Request request) {

        request.getHeaders().get("Authorization").ifPresent(s -> {
            log.info("Authorization:: {}", s);
        });

        return catalogService.getCatalogs(
                request.queryString("filter"),
                request.queryString("limit"),
                request.queryString("offset")
        )
                .thenApply(r -> ok(Json.toJson(r)));
    }
    public CompletionStage<Result> getCatalog(String id) {
        return catalogService.getCatalog(id)
                .thenApply(r -> ok(Json.toJson(r)));
    }
    public CompletionStage<Result> createCatalog(Http.Request request) {
        request.getHeaders().get("Authorization").ifPresent(s -> {
            log.info("Authorization:: {}", s);
        });
        request.getHeaders().get("x-user-id").ifPresent(s -> {
            log.info("x-user-id:: {}", s);
        });
        CreateCatalogRequest createCatalogRequest = Json.fromJson(request.body().asJson(), CreateCatalogRequest.class);
        return catalogService.createCatalog(createCatalogRequest)
                .thenApply(r -> ok(Json.toJson(r)));
    }
    public CompletionStage<Result> updateCatalog(Http.Request request){
        request.getHeaders().get("Authorization").ifPresent(s -> {
            log.info("Authorization:: {}", s);
        });
        request.getHeaders().get("x-user-id").ifPresent(s -> {
            log.info("x-user-id:: {}", s);
        });
        UpdateCatalogRequest updateCatalogRequest = Json.fromJson(request.body().asJson(), UpdateCatalogRequest.class);
        return catalogService.updateCatalog(updateCatalogRequest)
                .thenApply(r -> ok(Json.toJson(r)));
    }

    public CompletionStage<Result> patchCatalog(Http.Request request, String id){
        request.getHeaders().get("Authorization").ifPresent(s -> {
            log.info("Authorization:: {}", s);
        });
        request.getHeaders().get("x-user-id").ifPresent(s -> {
            log.info("x-user-id:: {}", s);
        });
        PatchCatalogRequest patchCatalogRequest = Json.fromJson(request.body().asJson(), PatchCatalogRequest.class);
        return catalogService.patchCatalog(patchCatalogRequest, id)
                .thenApply(r -> ok(Json.toJson(r)));
    }
    public CompletionStage<Result> deleteCatalog(String id) {
        return catalogService.deleteCatalog(id)
                .thenApply(r -> ok(Json.toJson(r)));
    }
    public CompletionStage<Result> importCsvCatalog(Http.Request request) {
        request.getHeaders().get("Authorization").ifPresent(s -> {
            log.info("Authorization:: {}", s);
        });
        Http.MultipartFormData<Files.TemporaryFile> body = request.body().asMultipartFormData();
        Optional<Http.MultipartFormData.FilePart<Files.TemporaryFile>> filePart = body.getFiles().stream().findFirst();
        List<CsvCatalog> csvCatalogs = CsvUtil.getRecords(filePart.get(), CsvCatalog.class);
        return catalogService.importCsvCatalog(csvCatalogs)
                .thenApply(r -> ok(Json.toJson(r)));
    }
    public CompletionStage<Result> exportCsvCatalog(){
        String fileName = "catalog_output.csv";
        return catalogService.exportCsvCatalog(fileName)
                .thenApply(r -> ok(r)
                        .withHeader("Content-Disposition",
                        String.format("attachment; filename=\"%s\"", fileName)).as("text/csv")
                );
    }
}