package com.retisio.arc.ext.message.catalog;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.retisio.arc.message.catalog.CatalogMessage;
import lombok.Value;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "event", defaultImpl = Void.class)
@JsonSubTypes({
        @JsonSubTypes.Type(ExtCatalogMessage.CatalogActivatedMessage.class),
})
public abstract class ExtCatalogMessage extends CatalogMessage {

    public ExtCatalogMessage(String catalogId){
        super(catalogId);
    }

    @JsonTypeName(value = "catalog-activated")
    @Value
    public final static class CatalogActivatedMessage extends ExtCatalogMessage {

        public final CatalogMessage.Catalog catalog;

        public CatalogActivatedMessage(CatalogMessage.Catalog catalog) {
            super(catalog.getCatalogId());
            this.catalog = catalog;
        }
    }

}
