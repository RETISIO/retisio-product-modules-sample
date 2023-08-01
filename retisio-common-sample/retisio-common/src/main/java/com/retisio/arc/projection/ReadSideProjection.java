package com.retisio.arc.projection;

import akka.actor.typed.ActorSystem;
import akka.cluster.sharding.typed.javadsl.ShardedDaemonProcess;
import akka.japi.Pair;
import akka.persistence.query.Offset;
import akka.persistence.query.typed.EventEnvelope;
import akka.persistence.r2dbc.query.javadsl.R2dbcReadJournal;
import akka.projection.Projection;
import akka.projection.ProjectionBehavior;
import akka.projection.ProjectionId;
import akka.projection.eventsourced.javadsl.EventSourcedProvider;
import akka.projection.javadsl.SourceProvider;
import akka.projection.r2dbc.R2dbcProjectionSettings;
import akka.projection.r2dbc.javadsl.R2dbcHandler;
import akka.projection.r2dbc.javadsl.R2dbcProjection;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
public class ReadSideProjection<T> {

    public static<T> void init(int count,
                               ActorSystem system,
                               String entityType,
                               String name,
                               String key,
                               R2dbcHandler<EventEnvelope<T>> handler) {

        List<Pair<Integer, Integer>> sliceRanges =
                EventSourcedProvider.sliceRanges(
                        system, R2dbcReadJournal.Identifier(), count);

        ShardedDaemonProcess.get(system)
                .init(
                        ProjectionBehavior.Command.class,
                        name,
                        sliceRanges.size(),
                        i -> ProjectionBehavior.create(createProjection(system, entityType, name, key, sliceRanges.get(i), handler)),
                        ProjectionBehavior.stopMessage());
    }

    private static<T> Projection<EventEnvelope<T>> createProjection(
            ActorSystem<?> system,
            String entityType,
            String name,
            String key,
            Pair<Integer, Integer> sliceRange,
            R2dbcHandler<EventEnvelope<T>> handler) {
        int minSlice = sliceRange.first();
        int maxSlice = sliceRange.second();

        SourceProvider<Offset, EventEnvelope<T>> sourceProvider =
                EventSourcedProvider.eventsBySlices(
                        system, R2dbcReadJournal.Identifier(), entityType, minSlice, maxSlice);

        ProjectionId projectionId =
                ProjectionId.of(name, key + minSlice + "-" + maxSlice);
        Optional<R2dbcProjectionSettings> settings = Optional.empty();

        int saveOffsetAfterEnvelopes = 100;
        Duration saveOffsetAfterDuration = Duration.ofMillis(500);
        log.info("{} init()..................", name);
        return R2dbcProjection.atLeastOnce(
                        projectionId, settings, sourceProvider, () -> handler, system)
                        .withSaveOffset(saveOffsetAfterEnvelopes, saveOffsetAfterDuration);
    }
}
