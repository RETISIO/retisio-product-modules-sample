package com.retisio.arc.ext.aggregate.catalog;

import com.retisio.arc.aggregate.catalog.Catalog;
import com.retisio.arc.aggregate.catalog.CatalogEvent;
import com.retisio.arc.aggregate.catalog.CatalogState;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class ExtCatalogState {

    public static CatalogState activateCatalog(CatalogState state, ExtCatalogEvent.CatalogActivated event){
        log.info("ExtCatalogState .... activateCatalog::{}", event.catalogId);
        return new CatalogState(
                Optional.of(
                        new Catalog(
                                event.catalogId,
                                state.catalog.get().getCatalogName(),
                                true,
                                state.catalog.get().getDynProps(),
                                false
                        )
                )
        );
    }
}
