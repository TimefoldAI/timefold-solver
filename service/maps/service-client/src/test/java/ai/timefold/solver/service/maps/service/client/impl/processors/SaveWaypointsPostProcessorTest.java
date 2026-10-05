package ai.timefold.solver.service.maps.service.client.impl.processors;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import ai.timefold.solver.service.definition.api.domain.Metadata;
import ai.timefold.solver.service.definition.internal.events.InitSolutionEvent;
import ai.timefold.solver.service.maps.api.model.Location;
import ai.timefold.solver.service.maps.api.model.Waypoints;
import ai.timefold.solver.service.maps.service.client.impl.WaypointsServiceImpl;
import ai.timefold.solver.service.maps.service.client.impl.error.MapServiceIllegalArgumentException;
import ai.timefold.solver.service.maps.service.client.util.DummyStorageService;
import ai.timefold.solver.service.maps.service.client.util.MapServiceInvocationCounter;
import ai.timefold.solver.service.maps.service.client.util.RemoteMapServiceConfigurationProfile;
import ai.timefold.solver.service.maps.service.client.util.SampleModel;
import ai.timefold.solver.service.maps.service.client.util.WaypointsCallControl;
import ai.timefold.solver.service.maps.service.test.api.MapServiceApiWiremockExtensions;

import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@QuarkusTestResource(MapServiceApiWiremockExtensions.class)
@TestProfile(RemoteMapServiceConfigurationProfile.class)
class SaveWaypointsPostProcessorTest {

    @Inject
    SaveWaypointsPostProcessor postProcessor;

    @Inject
    WaypointsServiceImpl waypointsService;

    @Inject
    DummyStorageService storageService;

    @Inject
    MapServiceInvocationCounter mapServiceInvocationCounter;

    @Inject
    WaypointsCallControl callControl;

    @BeforeEach
    void prepare() {
        callControl.releaseHeldCalls();
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> !callControl.hasCallsInFlight());
        mapServiceInvocationCounter.resetWaypointsInvocationCounter();
        callControl.reset();
    }

    @Test
    void releasesTheRunAfterItsWaypointsAreStored() {
        var metadata = startRun(new SampleModel(List.of(new Location(0, 0), new Location(1, 1))));

        postProcessor.process(null, null, metadata.getId());
        var storedWaypointsList = List.of(new Waypoints("id_0", List.of(new Location(7, 7))));
        storageService.storeWaypoints(metadata.getId(), storedWaypointsList);

        Assertions.assertThat(waypointsService.getWaypoints(metadata.getId(), Set.of())).isEqualTo(storedWaypointsList);
    }

    @Test
    void keepsTheRunWhenTheWaypointsFetchFails() {
        var metadata = startRun(new SampleModel(List.of(new Location(0, 0), new Location(1, 1))));
        callControl.failNextCalls(1);

        Assertions.assertThatThrownBy(() -> postProcessor.process(null, null, metadata.getId()))
                .isInstanceOf(MapServiceIllegalArgumentException.class);

        Assertions.assertThat(waypointsService.getWaypoints(metadata.getId(), Set.of()))
                .extracting(Waypoints::id).containsExactly("id_0");
        Assertions.assertThat(mapServiceInvocationCounter.getWaypointsInvocationCounter()).isEqualTo(2);
    }

    @Test
    void keepsARunWithoutRoutes() {
        var metadata = startRun(new SampleModel(List.of()));

        postProcessor.process(null, null, metadata.getId());

        Assertions.assertThat(waypointsService.getWaypoints(metadata.getId(), Set.of())).isEmpty();
    }

    private Metadata<?> startRun(SampleModel sampleModel) {
        var metadata = new Metadata<>();
        waypointsService.onInitSolution(new InitSolutionEvent(metadata, sampleModel, null, null, null, null));
        return metadata;
    }
}
