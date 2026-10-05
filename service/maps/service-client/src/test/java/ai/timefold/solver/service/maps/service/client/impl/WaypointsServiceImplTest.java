package ai.timefold.solver.service.maps.service.client.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import ai.timefold.solver.service.definition.api.domain.Metadata;
import ai.timefold.solver.service.definition.internal.error.ItemNotFoundException;
import ai.timefold.solver.service.definition.internal.events.BestSolutionEvent;
import ai.timefold.solver.service.definition.internal.events.FinalBestSolutionEvent;
import ai.timefold.solver.service.definition.internal.events.InitSolutionEvent;
import ai.timefold.solver.service.definition.internal.events.ItemFailed;
import ai.timefold.solver.service.definition.internal.events.ItemStarted;
import ai.timefold.solver.service.maps.api.model.Location;
import ai.timefold.solver.service.maps.api.model.Waypoints;
import ai.timefold.solver.service.maps.service.client.api.MapService;
import ai.timefold.solver.service.maps.service.client.impl.error.MapServiceIllegalArgumentException;
import ai.timefold.solver.service.maps.service.client.util.DummyStorageService;
import ai.timefold.solver.service.maps.service.client.util.MapServiceInvocationCounter;
import ai.timefold.solver.service.maps.service.client.util.RemoteMapServiceConfigurationProfile;
import ai.timefold.solver.service.maps.service.client.util.SampleModel;
import ai.timefold.solver.service.maps.service.client.util.WaypointsCallControl;
import ai.timefold.solver.service.maps.service.integration.api.WaypointsExtractorBase;
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
public class WaypointsServiceImplTest {

    @Inject
    WaypointsServiceImpl enricher;

    @Inject
    MapServiceInvocationCounter mapServiceInvocationCounter;

    @Inject
    WaypointsCallControl callControl;

    @Inject
    MapService mapService;

    @Inject
    Instance<WaypointsExtractorBase> waypointsExtractor;

    @Inject
    MapServiceOptionsSupplier optionsSupplier;

    @Inject
    DummyStorageService storageService;

    @BeforeEach
    public void prepare() {
        callControl.releaseHeldCalls();
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> !callControl.hasCallsInFlight());
        mapServiceInvocationCounter.resetWaypointsInvocationCounter();
        callControl.reset();
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
    void fetchesARouteOnlyWhenItsStopsChange() {
        Location l1 = new Location(0, 0);
        Location l2 = new Location(1, 1);

        Metadata<?> metadata = new Metadata<>();

        SampleModel sampleModel = new SampleModel(List.of(l1, l2));
        enricher.onInitSolution(new InitSolutionEvent(metadata, sampleModel, null, null, null, null));

        List<Waypoints> waypoints = enricher.getWaypoints(metadata.getId(), Set.of());

        Assertions.assertThat(waypoints.size()).isEqualTo(1);
        Assertions.assertThat(mapServiceInvocationCounter.getWaypointsInvocationCounter()).isEqualTo(1);

        waypoints = enricher.getWaypoints(metadata.getId(), Set.of());

        Assertions.assertThat(waypoints.size()).isEqualTo(1);
        Assertions.assertThat(mapServiceInvocationCounter.getWaypointsInvocationCounter()).isEqualTo(1);

        enricher.onBestSolution(new BestSolutionEvent(metadata, sampleModel, null, null, null, null));
        waypoints = enricher.getWaypoints(metadata.getId(), Set.of());

        Assertions.assertThat(waypoints.size()).isEqualTo(1);
        Assertions.assertThat(mapServiceInvocationCounter.getWaypointsInvocationCounter()).isEqualTo(1);

        enricher.onFinalBestSolution(new FinalBestSolutionEvent(metadata, sampleModel, null, null, null));
        waypoints = enricher.getWaypoints(metadata.getId(), Set.of());

        Assertions.assertThat(waypoints.size()).isEqualTo(1);
        Assertions.assertThat(mapServiceInvocationCounter.getWaypointsInvocationCounter()).isEqualTo(1);
    }

    @Test
    void fetchesOnlyTheRequestedRoutes() {
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

        waypoints = enricher.getWaypoints(metadata.getId(), Set.of("id_0"));

        Assertions.assertThat(waypoints.size()).isEqualTo(1);
        Assertions.assertThat(waypoints.getFirst().waypoints().size()).isEqualTo(2);
        Assertions.assertThat(waypoints.getFirst().waypoints()).contains(l1, l2);
        Assertions.assertThat(mapServiceInvocationCounter.getWaypointsInvocationCounter()).isEqualTo(1);

        waypoints = enricher.getWaypoints(metadata.getId(), Set.of("id_1"));

        Assertions.assertThat(waypoints.size()).isEqualTo(1);
        Assertions.assertThat(waypoints.getFirst().waypoints().size()).isEqualTo(2);
        Assertions.assertThat(waypoints.getFirst().waypoints()).contains(l3, l4);
        Assertions.assertThat(mapServiceInvocationCounter.getWaypointsInvocationCounter()).isEqualTo(2);

        waypoints = enricher.getWaypoints(metadata.getId(), Set.of("id_0", "id_1"));

        Assertions.assertThat(waypoints.size()).isEqualTo(2);
        Assertions.assertThat(waypoints.get(0).waypoints().size()).isEqualTo(2);
        Assertions.assertThat(waypoints.get(0).waypoints()).contains(l1, l2);

        Assertions.assertThat(waypoints.get(1).waypoints().size()).isEqualTo(2);
        Assertions.assertThat(waypoints.get(1).waypoints()).contains(l3, l4);
        Assertions.assertThat(mapServiceInvocationCounter.getWaypointsInvocationCounter()).isEqualTo(2);

        enricher.onBestSolution(new BestSolutionEvent(metadata, sampleModel, null, null, null, null));
        waypoints = enricher.getWaypoints(metadata.getId(), Set.of("id_0"));

        Assertions.assertThat(waypoints.size()).isEqualTo(1);
        Assertions.assertThat(waypoints.getFirst().waypoints().size()).isEqualTo(2);
        Assertions.assertThat(waypoints.getFirst().waypoints()).contains(l1, l2);
        Assertions.assertThat(mapServiceInvocationCounter.getWaypointsInvocationCounter()).isEqualTo(2);

        waypoints = enricher.getWaypoints(metadata.getId(), Set.of("id_1"));

        Assertions.assertThat(waypoints.size()).isEqualTo(1);
        Assertions.assertThat(waypoints.getFirst().waypoints().size()).isEqualTo(2);
        Assertions.assertThat(waypoints.getFirst().waypoints()).contains(l3, l4);
        Assertions.assertThat(mapServiceInvocationCounter.getWaypointsInvocationCounter()).isEqualTo(2);

        enricher.onBestSolution(new BestSolutionEvent(metadata, sampleModel, null, null, null, null));
        waypoints = enricher.getWaypoints(metadata.getId(), Set.of("id_0", "id_1"));

        Assertions.assertThat(waypoints.size()).isEqualTo(2);
        Assertions.assertThat(waypoints.get(0).waypoints().size()).isEqualTo(2);
        Assertions.assertThat(waypoints.get(0).waypoints()).contains(l1, l2);

        Assertions.assertThat(waypoints.get(1).waypoints().size()).isEqualTo(2);
        Assertions.assertThat(waypoints.get(1).waypoints()).contains(l3, l4);
        Assertions.assertThat(mapServiceInvocationCounter.getWaypointsInvocationCounter()).isEqualTo(2);
    }

    @Test
    void fetchesOnlyTheChangedRouteAfterABestSolution() {
        var metadata = new Metadata<>();
        enricher.onInitSolution(new InitSolutionEvent(metadata, twoRoutes(new Location(3, 3)), null, null, null, null));
        enricher.getWaypoints(metadata.getId(), Set.of());

        enricher.onBestSolution(new BestSolutionEvent(metadata, twoRoutes(new Location(4, 4)), null, null, null, null));
        var waypointsList = enricher.getWaypoints(metadata.getId(), Set.of());

        Assertions.assertThat(mapServiceInvocationCounter.getWaypointsInvocationCounter()).isEqualTo(3);
        Assertions.assertThat(waypointsList.get(1).waypoints()).contains(new Location(4, 4));
    }

    @Test
    void dropsAVehicleThatLeavesTheSolution() {
        var metadata = new Metadata<>();
        enricher.onInitSolution(new InitSolutionEvent(metadata, twoRoutes(new Location(3, 3)), null, null, null, null));
        enricher.onBestSolution(new BestSolutionEvent(metadata,
                new SampleModel(List.of(new Location(0, 0), new Location(1, 1))), null, null, null, null));

        Assertions.assertThat(enricher.getWaypoints(metadata.getId(), Set.of()))
                .extracting(Waypoints::id).containsExactly("id_0");
    }

    @Test
    void fetchesRoutesInParallelUpToTheLimit() {
        callControl.setDelayMillis(200);
        var metadata = new Metadata<>();
        enricher.onInitSolution(new InitSolutionEvent(metadata, routes(16), null, null, null, null));

        enricher.getWaypoints(metadata.getId(), Set.of());

        Assertions.assertThat(callControl.getMaxConcurrentCalls()).isBetween(2, 8);
        Assertions.assertThat(mapServiceInvocationCounter.getWaypointsInvocationCounter()).isEqualTo(16);
    }

    @Test
    void sharesOneFetchBetweenConcurrentRequests() throws Exception {
        callControl.setDelayMillis(200);
        var metadata = new Metadata<>();
        enricher.onInitSolution(new InitSolutionEvent(metadata, routes(4), null, null, null, null));

        var otherRequest = CompletableFuture.runAsync(() -> enricher.getWaypoints(metadata.getId(), Set.of()));
        enricher.getWaypoints(metadata.getId(), Set.of());
        otherRequest.get(5, TimeUnit.SECONDS);

        Assertions.assertThat(mapServiceInvocationCounter.getWaypointsInvocationCounter()).isEqualTo(4);
    }

    @Test
    void retriesARouteAfterAFailure() {
        var metadata = new Metadata<>();
        enricher.onInitSolution(new InitSolutionEvent(metadata, routes(1), null, null, null, null));
        callControl.failNextCalls(1);

        Assertions.assertThatThrownBy(() -> enricher.getWaypoints(metadata.getId(), Set.of()))
                .isInstanceOf(MapServiceIllegalArgumentException.class);
        Assertions.assertThat(enricher.getWaypoints(metadata.getId(), Set.of())).hasSize(1);
        Assertions.assertThat(mapServiceInvocationCounter.getWaypointsInvocationCounter()).isEqualTo(2);
    }

    @Test
    void retriesAFailedMapCallInsideOneRequest() {
        var metadata = new Metadata<>();
        enricher.onInitSolution(new InitSolutionEvent(metadata, routes(1), null, null, null, null));
        callControl.failNextCallsRetryably(1);

        Assertions.assertThat(enricher.getWaypoints(metadata.getId(), Set.of())).hasSize(1);
        Assertions.assertThat(mapServiceInvocationCounter.getWaypointsInvocationCounter()).isEqualTo(2);
    }

    @Test
    void rejectsAParallelismBelowOne() {
        Assertions.assertThatThrownBy(() -> new WaypointsServiceImpl(null, null, null, null, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("waypointsParallelism (0)");
    }

    @Test
    void returnsTheNewRouteWhenABestSolutionArrivesDuringAFetch() throws Exception {
        callControl.setDelayMillis(300);
        var metadata = new Metadata<>();
        enricher.onInitSolution(new InitSolutionEvent(metadata, twoRoutes(new Location(3, 3)), null, null, null, null));
        var staleRequest = CompletableFuture.supplyAsync(() -> enricher.getWaypoints(metadata.getId(), Set.of("id_1")));
        Awaitility.await().until(() -> mapServiceInvocationCounter.getWaypointsInvocationCounter() == 1);

        enricher.onBestSolution(new BestSolutionEvent(metadata, twoRoutes(new Location(4, 4)), null, null, null, null));
        var freshWaypointsList = enricher.getWaypoints(metadata.getId(), Set.of("id_1"));

        Assertions.assertThat(freshWaypointsList.getFirst().waypoints()).contains(new Location(4, 4));
        Assertions.assertThat(staleRequest.get(5, TimeUnit.SECONDS).getFirst().waypoints()).contains(new Location(3, 3));
    }

    @Test
    void fetchesAChangedRouteInTheBackgroundForAViewedRun() {
        var metadata = new Metadata<>();
        enricher.onInitSolution(new InitSolutionEvent(metadata, twoRoutes(new Location(3, 3)), null, null, null, null));
        enricher.getWaypoints(metadata.getId(), Set.of());

        enricher.onBestSolution(new BestSolutionEvent(metadata, twoRoutes(new Location(4, 4)), null, null, null, null));

        Awaitility.await().atMost(Duration.ofSeconds(5))
                .until(() -> mapServiceInvocationCounter.getWaypointsInvocationCounter() == 3);
        enricher.getWaypoints(metadata.getId(), Set.of());
        Assertions.assertThat(mapServiceInvocationCounter.getWaypointsInvocationCounter()).isEqualTo(3);
    }

    @Test
    void doesNotFetchInTheBackgroundForAnUnviewedRun() {
        var metadata = new Metadata<>();
        enricher.onInitSolution(new InitSolutionEvent(metadata, twoRoutes(new Location(3, 3)), null, null, null, null));
        enricher.onBestSolution(new BestSolutionEvent(metadata, twoRoutes(new Location(4, 4)), null, null, null, null));

        Awaitility.await().during(Duration.ofMillis(500)).atMost(Duration.ofSeconds(1))
                .until(() -> mapServiceInvocationCounter.getWaypointsInvocationCounter() == 0);
    }

    @Test
    void coalescesABurstOfBestSolutions() {
        var metadata = new Metadata<>();
        enricher.onInitSolution(new InitSolutionEvent(metadata, twoRoutes(new Location(3, 3)), null, null, null, null));
        enricher.getWaypoints(metadata.getId(), Set.of());
        callControl.setDelayMillis(300);

        for (int i = 4; i < 9; i++) {
            enricher.onBestSolution(new BestSolutionEvent(metadata, twoRoutes(new Location(i, i)), null, null, null, null));
        }

        Awaitility.await().atMost(Duration.ofSeconds(5))
                .until(() -> mapServiceInvocationCounter.getWaypointsInvocationCounter() == 4);
        Assertions.assertThat(enricher.getWaypoints(metadata.getId(), Set.of("id_1")).getFirst().waypoints())
                .contains(new Location(8, 8));
        Assertions.assertThat(mapServiceInvocationCounter.getWaypointsInvocationCounter()).isEqualTo(4);
    }

    @Test
    void retriesARouteWhoseBackgroundFetchFailed() {
        var metadata = new Metadata<>();
        enricher.onInitSolution(new InitSolutionEvent(metadata, twoRoutes(new Location(3, 3)), null, null, null, null));
        enricher.getWaypoints(metadata.getId(), Set.of());
        callControl.failNextCalls(1);

        enricher.onBestSolution(new BestSolutionEvent(metadata, twoRoutes(new Location(4, 4)), null, null, null, null));

        Awaitility.await().atMost(Duration.ofSeconds(5)).ignoreExceptions()
                .until(() -> enricher.getWaypoints(metadata.getId(), Set.of("id_1")).getFirst().waypoints()
                        .contains(new Location(4, 4)));
        Assertions.assertThat(mapServiceInvocationCounter.getWaypointsInvocationCounter()).isEqualTo(4);

        enricher.onBestSolution(new BestSolutionEvent(metadata, twoRoutes(new Location(5, 5)), null, null, null, null));

        Awaitility.await().atMost(Duration.ofSeconds(5))
                .until(() -> mapServiceInvocationCounter.getWaypointsInvocationCounter() == 5);
    }

    @Test
    void keepsHandlingBestSolutionsWhenTheBackgroundFetchCannotStart() {
        var waypointsService = new WaypointsServiceImpl(mapService, waypointsExtractor, optionsSupplier, storageService, 1);
        var metadata = new Metadata<>();
        waypointsService.onInitSolution(
                new InitSolutionEvent(metadata, twoRoutes(new Location(3, 3)), null, null, null, null));
        waypointsService.getWaypoints(metadata.getId(), Set.of());
        waypointsService.shutdownWaypointsExecutor();

        for (int i = 4; i < 6; i++) {
            var bestSolutionEvent = new BestSolutionEvent(metadata, twoRoutes(new Location(i, i)), null, null, null, null);
            Assertions.assertThatCode(() -> waypointsService.onBestSolution(bestSolutionEvent))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void readsStoredWaypointsAfterTheRunIsReleased() {
        var metadata = new Metadata<>();
        enricher.onInitSolution(new InitSolutionEvent(metadata, twoRoutes(new Location(3, 3)), null, null, null, null));
        enricher.getWaypoints(metadata.getId(), Set.of());
        storageService.storeWaypoints(metadata.getId(), List.of(new Waypoints("id_0", List.of(new Location(7, 7))),
                new Waypoints("id_1", List.of(new Location(8, 8)))));

        enricher.releaseFinishedRun(metadata.getId());

        Assertions.assertThat(enricher.getWaypoints(metadata.getId(), Set.of("id_1")))
                .singleElement().extracting(Waypoints::waypoints).isEqualTo(List.of(new Location(8, 8)));
        Assertions.assertThat(mapServiceInvocationCounter.getWaypointsInvocationCounter()).isEqualTo(2);
    }

    @Test
    void reportsNotFoundForAFailedRunWithoutStoredWaypoints() {
        var metadata = new Metadata<>();
        enricher.onInitSolution(new InitSolutionEvent(metadata, twoRoutes(new Location(3, 3)), null, null, null, null));

        enricher.onFailed(new ItemFailed(metadata, new IllegalStateException("Solver failed."), null, null));

        Assertions.assertThatThrownBy(() -> enricher.getWaypoints(metadata.getId(), Set.of()))
                .isInstanceOf(ItemNotFoundException.class);
    }

    @Test
    void ignoresABestSolutionThatArrivesAfterTheRunIsReleased() {
        var metadata = new Metadata<>();
        enricher.onInitSolution(new InitSolutionEvent(metadata, twoRoutes(new Location(3, 3)), null, null, null, null));
        storageService.storeWaypoints(metadata.getId(), enricher.getWaypoints(metadata.getId(), Set.of()));
        enricher.releaseFinishedRun(metadata.getId());

        enricher.onBestSolution(new BestSolutionEvent(metadata, twoRoutes(new Location(4, 4)), null, null, null, null));

        Assertions.assertThat(enricher.getWaypoints(metadata.getId(), Set.of("id_1")).getFirst().waypoints())
                .contains(new Location(3, 3));
        Assertions.assertThat(mapServiceInvocationCounter.getWaypointsInvocationCounter()).isEqualTo(2);
    }

    @Test
    void ignoresABestSolutionThatArrivesAfterFailure() {
        var metadata = new Metadata<>();
        enricher.onInitSolution(new InitSolutionEvent(metadata, twoRoutes(new Location(3, 3)), null, null, null, null));
        enricher.onFailed(new ItemFailed(metadata, new IllegalStateException("Solver failed."), null, null));

        enricher.onBestSolution(new BestSolutionEvent(metadata, twoRoutes(new Location(4, 4)), null, null, null, null));

        Assertions.assertThatThrownBy(() -> enricher.getWaypoints(metadata.getId(), Set.of()))
                .isInstanceOf(ItemNotFoundException.class);
        Assertions.assertThat(mapServiceInvocationCounter.getWaypointsInvocationCounter()).isZero();
    }

    @Test
    void tracksAReleasedRunAgainWhenItStartsAgain() {
        var metadata = new Metadata<>();
        enricher.onInitSolution(new InitSolutionEvent(metadata, twoRoutes(new Location(3, 3)), null, null, null, null));
        enricher.releaseFinishedRun(metadata.getId());

        enricher.onStarted(new ItemStarted(metadata, null, null, null));
        enricher.onBestSolution(new BestSolutionEvent(metadata, twoRoutes(new Location(4, 4)), null, null, null, null));

        Assertions.assertThat(enricher.getWaypoints(metadata.getId(), Set.of("id_1")).getFirst().waypoints())
                .contains(new Location(4, 4));
        Assertions.assertThat(mapServiceInvocationCounter.getWaypointsInvocationCounter()).isEqualTo(1);
    }

    @Test
    void doesNotStartATrailingBackgroundFetchForAReleasedRun() {
        var metadata = new Metadata<>();
        enricher.onInitSolution(new InitSolutionEvent(metadata, twoRoutes(new Location(3, 3)), null, null, null, null));
        enricher.getWaypoints(metadata.getId(), Set.of());
        callControl.holdCalls();
        enricher.onBestSolution(new BestSolutionEvent(metadata, twoRoutes(new Location(4, 4)), null, null, null, null));
        Awaitility.await().pollInterval(Duration.ofMillis(10)).atMost(Duration.ofSeconds(5))
                .until(callControl::hasCallsInFlight);

        enricher.onBestSolution(new BestSolutionEvent(metadata, twoRoutes(new Location(5, 5)), null, null, null, null));
        enricher.releaseFinishedRun(metadata.getId());
        callControl.releaseHeldCalls();

        Awaitility.await().pollInterval(Duration.ofMillis(10)).during(Duration.ofMillis(500)).atMost(Duration.ofSeconds(2))
                .until(() -> mapServiceInvocationCounter.getWaypointsInvocationCounter() == 3);
    }

    private static SampleModel twoRoutes(Location lastStop) {
        return new SampleModel(List.of(new Location(0, 0), new Location(1, 1), new Location(2, 2), lastStop));
    }

    private static SampleModel routes(int routeCount) {
        var stopList = new ArrayList<Location>();
        for (int i = 0; i < routeCount * 2; i++) {
            stopList.add(new Location(i, i));
        }
        return new SampleModel(stopList);
    }
}
