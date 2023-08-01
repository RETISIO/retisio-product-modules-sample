package com.retisio.arc.ext.service;

import com.retisio.arc.response.catalog.GetCatalogResponse;

import java.util.concurrent.CompletionStage;

public interface ExtCatalogService {
    public CompletionStage<GetCatalogResponse> deleteCatalog(String catalogId);
    public CompletionStage<GetCatalogResponse> activateCatalog(String catalogId);
}
