package com.retisio.arc.aggregate.catalog;

import akka.Done;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.Entity;
import akka.cluster.sharding.typed.javadsl.EntityTypeKey;
import akka.persistence.typed.PersistenceId;
import akka.persistence.typed.javadsl.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CatalogAggregate extends EventSourcedBehaviorWithEnforcedReplies<CatalogCommand, CatalogEvent, CatalogState> {

    //----------------------------
    public static EntityTypeKey<CatalogCommand> ENTITY_TYPE_KEY = EntityTypeKey.create(CatalogCommand.class, "CatalogAggregate");

    static Integer numberOfEvents;
    static Integer keepNSnapshots;

    public static void init(ClusterSharding clusterSharding, Integer numberOfEvents, Integer keepNSnapshots) {
        clusterSharding.init(
                Entity.of(
                        ENTITY_TYPE_KEY,
                        entityContext -> {
                            return create(entityContext.getEntityId(), numberOfEvents, keepNSnapshots);
                        }));
        log.info("CatalogAggregate init is completed ....");
    }

    public static Behavior<CatalogCommand> create(String entityId, Integer numberOfEvents, Integer keepNSnapshots) {
        return Behaviors.setup(
                ctx -> EventSourcedBehavior.start(new CatalogAggregate(PersistenceId.of(ENTITY_TYPE_KEY.name(), entityId), numberOfEvents, keepNSnapshots), ctx));
    }

    public CatalogAggregate(PersistenceId persistenceId, Integer numberOfEvents, Integer keepNSnapshots) {
        super(persistenceId);
        this.numberOfEvents = numberOfEvents;
        this.keepNSnapshots = keepNSnapshots;
    }
    //----------------------------------
    @Override
    public RetentionCriteria retentionCriteria() {
        return RetentionCriteria.snapshotEvery(numberOfEvents,
                keepNSnapshots).withDeleteEventsOnSnapshot();
    }
    //---------------------------------

    @Override
    public CatalogState emptyState() {
        return CatalogState.EMPTY;
    }

    @Override
    public CommandHandlerWithReply<CatalogCommand, CatalogEvent, CatalogState> commandHandler() {
        return customCommandHandlerWithReplyBuilder()
                .build();
    }

    private CommandHandlerWithReplyBuilderByState<CatalogCommand, CatalogEvent, CatalogState, CatalogState> getCommandHandlerWithReplyBuilderByState() {
        return newCommandHandlerWithReplyBuilder()
                .forAnyState()
                .onCommand(CatalogCommand.GetCatalog.class, (state, cmd) -> Effect()
                        .none()
                        .thenReply(cmd.getReplyTo(), __ -> state.catalog))
                .onCommand(CatalogCommand.CreateCatalog.class, (state, cmd) -> Effect()
                        .persist(CatalogEvent.CatalogCreated.getInstance(cmd))
                        .thenReply(cmd.getReplyTo(), __ -> Done.getInstance()))
                .onCommand(CatalogCommand.UpdateCatalog.class, (state, cmd) -> Effect()
                        .persist(CatalogEvent.CatalogUpdated.getInstance(cmd))
                        .thenReply(cmd.getReplyTo(), __ -> Done.getInstance()))
                .onCommand(CatalogCommand.PatchCatalog.class, (state, cmd) -> Effect()
                        .persist(CatalogEvent.CatalogPatched.getInstance(cmd))
                        .thenReply(cmd.getReplyTo(), __ -> Done.getInstance()))
                .onCommand(CatalogCommand.DeleteCatalog.class, (state, cmd) -> Effect()
                        .persist(CatalogEvent.CatalogDeleted.getInstance(cmd))
                        .thenReply(cmd.getReplyTo(), __ -> Done.getInstance()));
    }

    protected  CommandHandlerWithReplyBuilderByState<CatalogCommand, CatalogEvent, CatalogState, CatalogState> customCommandHandlerWithReplyBuilder() {
        return getCommandHandlerWithReplyBuilderByState();
    }

    @Override
    public EventHandler<CatalogState, CatalogEvent> eventHandler() {
        return customEventHandlerBuilder()
                .build();
    }

    private EventHandlerBuilderByState<CatalogState, CatalogState, CatalogEvent> getEventHandlerBuilderByState() {
        return newEventHandlerBuilder().
                forAnyState()
                .onEvent(CatalogEvent.CatalogCreated.class,
                        (state, evt) -> state.createCatalog(evt))
                .onEvent(CatalogEvent.CatalogUpdated.class,
                        (state, evt) -> state.updateCatalog(evt))
                .onEvent(CatalogEvent.CatalogPatched.class,
                        (state, evt) -> state.patchCatalog(evt))
                .onEvent(CatalogEvent.CatalogDeleted.class,
                        (state, evt) -> state.deleteCatalog(evt));
    }
    protected EventHandlerBuilderByState<CatalogState, CatalogState, CatalogEvent> customEventHandlerBuilder() {
        return getEventHandlerBuilderByState();
    }
    //-------------------------------
}
