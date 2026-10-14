package ai.timefold.solver.service.maps.service.client.impl.recording;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;

import ai.timefold.solver.service.definition.internal.MapEnrichmentContext;
import ai.timefold.solver.service.maps.api.model.Location;
import ai.timefold.solver.service.maps.api.model.travel.TravelDistance;
import ai.timefold.solver.service.maps.api.model.travel.TravelTime;
import ai.timefold.solver.service.maps.service.client.api.MapService;
import ai.timefold.solver.service.maps.service.client.api.TravelTimeMatrixEnricher;
import ai.timefold.solver.service.maps.service.client.impl.MapServiceOptionsSupplier;
import ai.timefold.solver.service.maps.service.client.impl.MapServiceProducer;
import ai.timefold.solver.service.maps.service.client.impl.bucketing.SingleTimeframeBucketing;
import ai.timefold.solver.service.maps.service.client.util.SampleModel;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Wires the real producer and enricher by hand: the Quarkus tests in this module replace the producer with a test
 * alternative, so they cannot cover how the replay is selected.
 */
class ReplayEnrichmentTest {

    /** The unzipped execution artifact of a recorded run, see {@code 00001-distances.json} in it. */
    private static final String RECORDING = "src/test/resources/replay-recording";

    @Test
    void enrichesFromRecording() {
        var mapEnrichmentContext = new MapEnrichmentContext();
        var enricher = new TravelTimeMatrixEnricher(produce(Optional.of(RECORDING)), carOptions(),
                mapEnrichmentContext, false);
        // A subset of the recorded locations would also work; here all of them, in a different order.
        Location l1 = new Location(1, 1);
        Location l0 = new Location(0, 0);
        Location l2 = new Location(2, 2);

        var enriched = enricher.enrich(new SampleModel(List.of(l1, l0, l2)));

        assertThat(l0.getTravelTimeTo(l1)).isEqualTo(TravelTime.of(111L));
        assertThat(l1.getTravelTimeTo(l0)).isEqualTo(TravelTime.of(222L));
        assertThat(l2.getTravelTimeTo(l1)).isEqualTo(TravelTime.of(666L));
        assertThat(l0.getDistanceTo(l1)).isEqualTo(TravelDistance.of(1111L));
        assertThat(l1.getDistanceTo(l2)).isEqualTo(TravelDistance.of(4444L));
        assertThat(enriched.getLocationsNotInMap()).containsExactly(l2);
        assertThat(mapEnrichmentContext.getResolvedMapLocation()).isEqualTo("us-georgia");
    }

    @Test
    void producesReplayWhenEnabled() {
        assertThat(produce(Optional.of(RECORDING))).isInstanceOf(ReplayMapService.class);
    }

    @Test
    void requiresReplayDirectory() {
        assertThatThrownBy(() -> produce(Optional.empty()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("timefold.platform.map-service.replay.directory");
    }

    private static MapService produce(Optional<String> replayDirectory) {
        // Only the replay path is exercised, so the remote client and its dependencies are not needed.
        return new MapServiceProducer(true, null, null, null, List.of(), false, false, Optional.empty(),
                new SingleTimeframeBucketing(), null, new ObjectMapper(), false, true, replayDirectory)
                .mapServiceProducer();
    }

    private static MapServiceOptionsSupplier carOptions() {
        return new MapServiceOptionsSupplier(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.of("local-tenant"), Optional.of("car"), Optional.empty());
    }
}
