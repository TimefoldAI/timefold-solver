package ai.timefold.solver.model.maps.service.client.impl.bucketing;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

/**
 * Maps an {@link OffsetDateTime} to a {@link Timeframe} <i>and</i> to a zero-based index into the ordered list
 * returned by {@link #allTimeframes()}. The map service client uses {@link Timeframe} for typed subset keys and the
 * wire option ({@code timeframe=morning}), while the solver-facing hot path uses the index for an O(1) array lookup.
 * <p>
 * Bucketing is an implementation detail of the maps service client; it is not exposed by {@code maps-api} or
 * {@code service-integration}. {@code Location} only receives a {@code ToIntFunction<OffsetDateTime>} derived from
 * {@link #indexOf(OffsetDateTime)}.
 * <p>
 * Implementations must be deterministic, stateless, and thread-safe. The invariant
 * {@code allTimeframes().get(indexOf(t)).equals(timeframeOf(t))} must hold for every {@code t}.
 */
public interface TimeframeBucketing {

    /**
     * @param time the instant for which to determine the timeframe; never {@code null}
     * @return the timeframe this instant belongs to; never {@code null}
     */
    Timeframe timeframeOf(OffsetDateTime time);

    /**
     * @param time the instant for which to determine the timeframe; never {@code null}
     * @return the zero-based position of this instant's timeframe in {@link #allTimeframes()}.
     */
    int indexOf(OffsetDateTime time);

    /**
     * @return every timeframe this bucketing can produce, in a stable order. The index of each entry in this list
     *         matches the value returned by {@link #indexOf(OffsetDateTime)} for any instant that maps to it.
     */
    List<Timeframe> allTimeframes();

    /**
     * @param from the start of the interval (inclusive); never {@code null}
     * @param to the end of the interval (exclusive); never {@code null}, must not be before {@code from}
     * @return the set of timeframes that the half-open interval {@code [from, to)} overlaps with;
     *         never empty — at minimum the timeframe of {@code from} is always included
     */
    Set<Timeframe> timeframesOf(OffsetDateTime from, OffsetDateTime to);

    /**
     * @return the timeframe used when a query doesn't carry a timestamp (e.g. the traffic-without-pruning path, where
     *         the maps service client appends this timeframe as an option to fetch a single traffic-aware matrix on the
     *         model's behalf). Implementations should pick a neutral representative bucket (e.g. mid-day) rather than
     *         an edge case. Never {@code null}; must always be a member of {@link #allTimeframes()}.
     */
    Timeframe defaultTimeframe();

}
