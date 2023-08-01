package com.retisio.arc.ext.aggregate.catalog;

import akka.Done;
import akka.actor.typed.ActorRef;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.retisio.arc.aggregate.catalog.CatalogCommand;
import com.retisio.arc.aggregate.catalog.CatalogEvent;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

public interface ExtCatalogCommand {
    @Value
    @JsonDeserialize
    @Slf4j
    public class ActivateCatalog implements CatalogCommand {
        String catalogId;
        ActorRef<Done> replyTo;

        @JsonCreator
        public ActivateCatalog(String catalogId, ActorRef<Done> replyTo) {
            this.catalogId = catalogId;
            this.replyTo = replyTo;
            log.info("ActivateCatalog ....");
        }
    }
}
