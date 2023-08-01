package com.retisio.arc.aggregate.catalog;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.retisio.arc.serializer.JsonSerializable;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.LinkedHashMap;

public abstract class CatalogEvent implements JsonSerializable {

    final public String catalogId;

    public CatalogEvent(String catalogId){
        this.catalogId = catalogId;
    }

    @Value
    @JsonDeserialize
    @Slf4j
    public final static class CatalogCreated extends CatalogEvent {

        String catalogName;
        Boolean active;
        LinkedHashMap<String, Serializable> dynProps;

        @JsonCreator
        private CatalogCreated(String catalogId, String catalogName, Boolean active, LinkedHashMap<String, Serializable> dynProps) {
            super(catalogId);
            this.catalogName = catalogName;
            this.active = active;
            this.dynProps = dynProps;
            log.info("CatalogCreated ....{}", catalogId);
        }

        static CatalogCreated getInstance(CatalogCommand.CreateCatalog cmd) {
            return new CatalogCreated(
                    cmd.getCatalogId(),
                    cmd.getCatalogName(),
                    cmd.getActive(),
                    cmd.getDynProps()
            );
        }
    }

    @Value
    @JsonDeserialize
    @Slf4j
    public final static class CatalogUpdated extends CatalogEvent {

        String catalogName;
        Boolean active;
        LinkedHashMap<String, Serializable> dynProps;

        @JsonCreator
        private CatalogUpdated(String catalogId, String catalogName, Boolean active, LinkedHashMap<String, Serializable> dynProps) {
            super(catalogId);
            this.catalogName = catalogName;
            this.active = active;
            this.dynProps = dynProps;
            log.info("CatalogUpdated ....{}", catalogId);
        }

        static CatalogUpdated getInstance(CatalogCommand.UpdateCatalog cmd) {
            return new CatalogUpdated(
                    cmd.getCatalogId(),
                    cmd.getCatalogName(),
                    cmd.getActive(),
                    cmd.getDynProps()
            );
        }
    }

    @Value
    @JsonDeserialize
    @Slf4j
    public final static class CatalogPatched extends CatalogEvent {

        String catalogName;
        Boolean active;
        LinkedHashMap<String, Serializable> dynProps;

        @JsonCreator
        private CatalogPatched(String catalogId, String catalogName, Boolean active, LinkedHashMap<String, Serializable> dynProps) {
            super(catalogId);
            this.catalogName = catalogName;
            this.active = active;
            this.dynProps = dynProps;
            log.info("CatalogPatched ....{}", catalogId);
        }

        static CatalogPatched getInstance(CatalogCommand.PatchCatalog cmd) {
            return new CatalogPatched(
                    cmd.getCatalogId(),
                    cmd.getCatalogName(),
                    cmd.getActive(),
                    cmd.getDynProps()
            );
        }
    }

    @Value
    @JsonDeserialize
    @Slf4j
    public final static class CatalogDeleted extends CatalogEvent {

        @JsonCreator
        private CatalogDeleted(String catalogId) {
            super(catalogId);
            log.info("CatalogDeleted ....{}", catalogId);
        }

        static CatalogDeleted getInstance(CatalogCommand.DeleteCatalog cmd) {
            return new CatalogDeleted(
                    cmd.getCatalogId()
            );
        }
    }
}
