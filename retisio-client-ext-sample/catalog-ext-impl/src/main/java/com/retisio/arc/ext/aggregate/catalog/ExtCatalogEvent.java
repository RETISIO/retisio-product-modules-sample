package com.retisio.arc.ext.aggregate.catalog;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.retisio.arc.aggregate.catalog.CatalogEvent;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

public interface ExtCatalogEvent {
    @Value
    @JsonDeserialize
    @Slf4j
    public final class CatalogActivated extends CatalogEvent {

        @JsonCreator
        private CatalogActivated(String catalogId) {
            super(catalogId);
            log.info("CatalogActivated ....");
        }
        public static CatalogActivated getInstance(ExtCatalogCommand.ActivateCatalog cmd) {
            return new CatalogActivated(
                    cmd.getCatalogId()
            );
        }
    }
}
