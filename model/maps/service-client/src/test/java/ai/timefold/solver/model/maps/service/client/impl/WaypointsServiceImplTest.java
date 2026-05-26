package ai.timefold.solver.model.maps.service.client.impl;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import ai.timefold.solver.model.definition.api.domain.Metadata;
import ai.timefold.solver.model.definition.internal.events.BestSolutionEvent;
import ai.timefold.solver.model.definition.internal.events.FinalBestSolutionEvent;
import ai.timefold.solver.model.definition.internal.events.InitSolutionEvent;
import ai.timefold.solver.model.maps.api.model.Location;
import ai.timefold.solver.model.maps.api.model.Waypoints;
import ai.timefold.solver.model.maps.service.client.util.MapServiceInvocationCounter;
import ai.timefold.solver.model.maps.service.client.util.RemoteMapServiceConfigurationProfile;
import ai.timefold.solver.model.maps.service.client.util.SampleModel;
import ai.timefold.solver.model.maps.service.test.api.MapServiceApiWiremockExtensions;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@QuarkusTestResource(MapServiceApiWiremockExtensions.class)
@TestProfile(RemoteMapServiceConfigurationProfile.class)
public class WaypointsServiceImplTest {

    @Inject
    WaypointsServiceImpl enricher;

    @Inject
    MapServiceInvocationCounter mapServiceInvocationCounter;

    @BeforeEach
    public void prepare() {
        mapServiceInvocationCounter.resetWaypointsInvocationCounter();
    }

    @Test
    void testRemoteConnectionWithMapServer() {
        Location l1 = new Location(0, 0);
        Location l2 = new Location(1, 1);

        Metadata<?> metadata = new Metadata<>();

        SampleModel sampleModel = new SampleModel(List.of(l1, l2));
        enricher.onInitSolution(new InitSolutionEvent(metadata, sampleModel, null, null, null, null));

        List<Waypoints> waypoints = enricher.getWaypoints(metadata.getId(), Set.of());

        Assertions.assertThat(waypoints.size()).isEqualTo(1);
        Assertions.assertThat(mapServiceInvocationCounter.getWaypointsInvocationCounter()).isEqualTo(1);
    }

    @Test
    void testOneLocationShouldReturnEmptyWaypoints() {
        Location l1 = new Location(0, 0);

        Metadata<?> metadata = new Metadata<>();
        SampleModel sampleModel = new SampleModel(List.of(l1));

        enricher.onInitSolution(new InitSolutionEvent(metadata, sampleModel, null, null, null, null));
        List<Waypoints> waypoints = enricher.getWaypoints(metadata.getId(), Set.of());

        Assertions.assertThat(waypoints.size()).isEqualTo(1);
        Assertions.assertThat(waypoints.getFirst().waypoints().size()).isEqualTo(1);
        Assertions.assertThat(waypoints.getFirst().waypoints().getFirst()).isEqualTo(l1);
    }

    @Test
    void testRemoteConnectionWithMapServerCalculateOnChangeOnly() {
        Location l1 = new Location(0, 0);
        Location l2 = new Location(1, 1);

        Metadata<?> metadata = new Metadata<>();

        SampleModel sampleModel = new SampleModel(List.of(l1, l2));
        enricher.onInitSolution(new InitSolutionEvent(metadata, sampleModel, null, null, null, null));

        List<Waypoints> waypoints = enricher.getWaypoints(metadata.getId(), Set.of());

        Assertions.assertThat(waypoints.size()).isEqualTo(1);
        Assertions.assertThat(mapServiceInvocationCounter.getWaypointsInvocationCounter()).isEqualTo(1);

        // second call to waypoints should return from cache and not call map service, so the counter should be still 1
        waypoints = enricher.getWaypoints(metadata.getId(), Set.of());

        Assertions.assertThat(waypoints.size()).isEqualTo(1);
        Assertions.assertThat(mapServiceInvocationCounter.getWaypointsInvocationCounter()).isEqualTo(1);

        // next send new best solution event
        enricher.onBestSolution(new BestSolutionEvent(metadata, sampleModel, null, null, null, null));
        // now waypoints should be recalculated as new solution was received
        waypoints = enricher.getWaypoints(metadata.getId(), Set.of());

        Assertions.assertThat(waypoints.size()).isEqualTo(1);
        Assertions.assertThat(mapServiceInvocationCounter.getWaypointsInvocationCounter()).isEqualTo(2);

        // doing it again should not recalculate
        waypoints = enricher.getWaypoints(metadata.getId(), Set.of());

        Assertions.assertThat(waypoints.size()).isEqualTo(1);
        Assertions.assertThat(mapServiceInvocationCounter.getWaypointsInvocationCounter()).isEqualTo(2);

        // finally send the final solution
        enricher.onFinalBestSolution(new FinalBestSolutionEvent(metadata, sampleModel, null, null, null));
        waypoints = enricher.getWaypoints(metadata.getId(), Set.of());

        Assertions.assertThat(waypoints.size()).isEqualTo(1);
        Assertions.assertThat(mapServiceInvocationCounter.getWaypointsInvocationCounter()).isEqualTo(3);
    }

    @Test
    void testRemoteConnectionWithMapServerCalculateOnChangeOnlyFiltered() {
        Location l1 = new Location(0, 0);
        Location l2 = new Location(1, 1);

        Location l3 = new Location(2, 2);
        Location l4 = new Location(3, 3);

        Metadata<?> metadata = new Metadata<>();
        SampleModel sampleModel = new SampleModel(List.of(l1, l2, l3, l4));

        enricher.onInitSolution(new InitSolutionEvent(metadata, sampleModel, null, null, null, null));
        List<Waypoints> waypoints = enricher.getWaypoints(metadata.getId(), Set.of("id_0"));

        Assertions.assertThat(waypoints.size()).isEqualTo(1);
        Assertions.assertThat(waypoints.getFirst().waypoints().size()).isEqualTo(2);
        Assertions.assertThat(waypoints.getFirst().waypoints()).contains(l1, l2);
        Assertions.assertThat(mapServiceInvocationCounter.getWaypointsInvocationCounter()).isEqualTo(1);

        // get it one again and it should be taken from the cache
        waypoints = enricher.getWaypoints(metadata.getId(), Set.of("id_0"));

        Assertions.assertThat(waypoints.size()).isEqualTo(1);
        Assertions.assertThat(waypoints.getFirst().waypoints().size()).isEqualTo(2);
        Assertions.assertThat(waypoints.getFirst().waypoints()).contains(l1, l2);
        Assertions.assertThat(mapServiceInvocationCounter.getWaypointsInvocationCounter()).isEqualTo(1);

        // now take the other one
        waypoints = enricher.getWaypoints(metadata.getId(), Set.of("id_1"));

        Assertions.assertThat(waypoints.size()).isEqualTo(1);
        Assertions.assertThat(waypoints.getFirst().waypoints().size()).isEqualTo(2);
        Assertions.assertThat(waypoints.getFirst().waypoints()).contains(l3, l4);
        Assertions.assertThat(mapServiceInvocationCounter.getWaypointsInvocationCounter()).isEqualTo(2);

        //now take both, should be returned from the cache
        waypoints = enricher.getWaypoints(metadata.getId(), Set.of("id_0", "id_1"));

        Assertions.assertThat(waypoints.size()).isEqualTo(2);
        Assertions.assertThat(waypoints.get(0).waypoints().size()).isEqualTo(2);
        Assertions.assertThat(waypoints.get(0).waypoints()).contains(l1, l2);

        Assertions.assertThat(waypoints.get(1).waypoints().size()).isEqualTo(2);
        Assertions.assertThat(waypoints.get(1).waypoints()).contains(l3, l4);
        Assertions.assertThat(mapServiceInvocationCounter.getWaypointsInvocationCounter()).isEqualTo(2);

        // next send new best solution event
        enricher.onBestSolution(new BestSolutionEvent(metadata, sampleModel, null, null, null, null));
        waypoints = enricher.getWaypoints(metadata.getId(), Set.of("id_0"));

        Assertions.assertThat(waypoints.size()).isEqualTo(1);
        Assertions.assertThat(waypoints.getFirst().waypoints().size()).isEqualTo(2);
        Assertions.assertThat(waypoints.getFirst().waypoints()).contains(l1, l2);
        Assertions.assertThat(mapServiceInvocationCounter.getWaypointsInvocationCounter()).isEqualTo(3);

        // now take the other one again after best solution event
        waypoints = enricher.getWaypoints(metadata.getId(), Set.of("id_1"));

        Assertions.assertThat(waypoints.size()).isEqualTo(1);
        Assertions.assertThat(waypoints.getFirst().waypoints().size()).isEqualTo(2);
        Assertions.assertThat(waypoints.getFirst().waypoints()).contains(l3, l4);
        Assertions.assertThat(mapServiceInvocationCounter.getWaypointsInvocationCounter()).isEqualTo(4);

        // once again send the best solution event and take both of them
        enricher.onBestSolution(new BestSolutionEvent(metadata, sampleModel, null, null, null, null));
        waypoints = enricher.getWaypoints(metadata.getId(), Set.of("id_0", "id_1"));

        Assertions.assertThat(waypoints.size()).isEqualTo(2);
        Assertions.assertThat(waypoints.get(0).waypoints().size()).isEqualTo(2);
        Assertions.assertThat(waypoints.get(0).waypoints()).contains(l1, l2);

        Assertions.assertThat(waypoints.get(1).waypoints().size()).isEqualTo(2);
        Assertions.assertThat(waypoints.get(1).waypoints()).contains(l3, l4);
        Assertions.assertThat(mapServiceInvocationCounter.getWaypointsInvocationCounter()).isEqualTo(6);
    }
}
