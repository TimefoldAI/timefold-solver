package ai.timefold.solver.service.maps.service.client.impl.bucketing;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

/**
 * Bucketing that maps every {@link OffsetDateTime} to a single timeframe. Intended for timeframe-independent providers
 * (for example, Haversine) where travel time and distance do not vary with time of day — the map service still needs
 * to return a matrix per timeframe key, so one key covers every lookup and {@link #indexOf(OffsetDateTime)} always
 * returns 0.
 */
public final class SingleTimeframeBucketing implements TimeframeBucketing {

    public static final Timeframe DEFAULT = new Timeframe("default");

    private static final List<Timeframe> ALL = List.of(DEFAULT);

    @Override
    public Timeframe timeframeOf(OffsetDateTime time) {
        return DEFAULT;
    }

    @Override
    public int indexOf(OffsetDateTime time) {
        return 0;
    }

    @Override
    public List<Timeframe> allTimeframes() {
        return ALL;
    }

    @Override
    public Set<Timeframe> timeframesOf(OffsetDateTime from, OffsetDateTime to) {
        return Set.of(DEFAULT);
    }

    @Override
    public Timeframe defaultTimeframe() {
        return DEFAULT;
    }

}
