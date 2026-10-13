package ai.timefold.solver.service.maps.service.client.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import ai.timefold.solver.service.maps.api.model.Location;
import ai.timefold.solver.service.maps.api.model.Waypoints;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class RouteTest {

    @Test
    void startsANewFetchAfterTheFetcherThrowsAnError() {
        var baseWaypoints = new Waypoints("id_0", List.of(new Location(0, 0)));
        var route = new Route(baseWaypoints);

        Assertions.assertThatThrownBy(() -> route.fetchOnce(() -> {
            throw new AssertionError("Injected fetcher error.");
        })).isInstanceOf(AssertionError.class);

        Assertions.assertThat(route.fetchOnce(() -> CompletableFuture.completedFuture(baseWaypoints)))
                .isCompletedWithValue(baseWaypoints);
    }
}
