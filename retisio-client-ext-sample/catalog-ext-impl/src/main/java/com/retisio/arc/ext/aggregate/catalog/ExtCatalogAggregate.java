package com.retisio.arc.ext.aggregate.catalog;

import akka.Done;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.Entity;
import akka.persistence.typed.PersistenceId;
import akka.persistence.typed.javadsl.CommandHandlerWithReplyBuilderByState;
import akka.persistence.typed.javadsl.EventHandlerBuilderByState;
import akka.persistence.typed.javadsl.EventSourcedBehavior;
import com.retisio.arc.aggregate.catalog.CatalogAggregate;
import com.retisio.arc.aggregate.catalog.CatalogCommand;
import com.retisio.arc.aggregate.catalog.CatalogEvent;
import com.retisio.arc.aggregate.catalog.CatalogState;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExtCatalogAggregate extends CatalogAggregate {

    public static void init(ClusterSharding clusterSharding, Integer numberOfEvents, Integer keepNSnapshots) {
        clusterSharding.init(
                Entity.of(
                    ENTITY_TYPE_KEY,
                    entityContext -> {
                        return Behaviors.setup(
                                ctx -> EventSourcedBehavior.start(
                                        new ExtCatalogAggregate(
                                            PersistenceId.of(ENTITY_TYPE_KEY.name(), entityContext.getEntityId()),
                                            numberOfEvents,
                                            keepNSnapshots
                                        ),
                                        ctx
                                )
                        );
                    })
        );
        log.info("ExtCatalogAggregate init is completed ....");
    }

    public ExtCatalogAggregate(PersistenceId persistenceId, Integer numberOfEvents, Integer keepNSnapshots) {
        super(persistenceId, numberOfEvents, keepNSnapshots);
    }
    //-------------------------------

    @Override
    public CommandHandlerWithReplyBuilderByState<CatalogCommand, CatalogEvent, CatalogState, CatalogState> customCommandHandlerWithReplyBuilder() {
        return getCommandHandlerWithReplyBuilderByState()
                .onCommand(ExtCatalogCommand.ActivateCatalog.class, (state, cmd) -> Effect()
                        .persist(ExtCatalogEvent.CatalogActivated.getInstance(cmd))
                        .thenReply(cmd.getReplyTo(), __ -> Done.getInstance()));
    }
    @Override
    public EventHandlerBuilderByState<CatalogState, CatalogState, CatalogEvent> customEventHandlerBuilder() {
        return getEventHandlerBuilderByState()
                .onEvent(ExtCatalogEvent.CatalogActivated.class,
                        (state, evt) -> ExtCatalogState.activateCatalog(state, evt));
    }

}
