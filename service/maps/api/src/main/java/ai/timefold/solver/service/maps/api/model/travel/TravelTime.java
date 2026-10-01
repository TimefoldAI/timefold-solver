package ai.timefold.solver.service.maps.api.model.travel;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.function.LongUnaryOperator;

import ai.timefold.solver.service.maps.api.model.Location;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Represents the travel time of a potentially unreachable route between two
 * {@link Location}-s.
 *
 * @param seconds The travel time in seconds. Important: the value is undefined when {@link #isReachable()} returns false
 *        and an attempt to retrieve it will throw an {@link IllegalStateException}.
 */
@NullMarked
public record TravelTime(long seconds) {

    // the representation of unreachable travel time must not leak through the API
    private static final long UNREACHABLE_TRAVEL_TIME = Long.MAX_VALUE;

    public static final TravelTime ZERO = new TravelTime(0);
    public static final TravelTime UNREACHABLE = new TravelTime(UNREACHABLE_TRAVEL_TIME);

    /**
     * Returns a new or shared instance of reachable travel time representing the given number of seconds.
     *
     * @param seconds the number of seconds
     * @return a new or shared instance of {@link TravelTime}
     */
    public static TravelTime of(long seconds) {
        return seconds == 0L ? ZERO : new TravelTime(seconds);
    }

    /**
     * Returns true if this travel time represents the duration of a reachable route.
     *
     * @return true If this travel time represents the duration of a reachable route, false otherwise.
     */
    public boolean isReachable() {
        return seconds < UNREACHABLE_TRAVEL_TIME;
    }

    /**
     * Returns the travel time in seconds. If the travel time represents an unreachable travel route time,
     * throws {@link IllegalStateException}.
     *
     * @return the number of seconds
     * @throws IllegalStateException when the travel time is not reachable
     */
    public long seconds() {
        if (!isReachable()) {
            throw new IllegalStateException("Cannot retrieve an unreachable TravelTime value.");
        }
        return seconds;
    }

    /**
     * Returns the travel time in seconds, if reachable. If not, returns zero.
     *
     * @return the number of reachable seconds
     */
    public long reachableSeconds() {
        return isReachable() ? seconds : 0L;
    }

    /**
     * Returns the given <code>travelStartTime</code> plus the number of {@link #seconds()} represented by this
     * {@link TravelTime} instance.
     * <p>
     * If this travel time represents an unreachable travel time, returns <code>travelStartTime</code> without any change.
     *
     * @param travelStartTime the date and time to add this travel time to
     * @return the given <code>travelStartTime</code> increased by this travel time if reachable; if unreachable, returns
     *         <code>travelStartTime</code> as is; null if <code>travelStartTime</code> is null
     */
    @Nullable
    public OffsetDateTime applyToTravelStart(@Nullable OffsetDateTime travelStartTime) {
        if (travelStartTime == null) {
            return null;
        }
        return isReachable() ? travelStartTime.plusSeconds(seconds) : travelStartTime;
    }

    /**
     * Returns a {@link TravelTime} instance with travel time seconds adjusted by the given function.
     *
     * @param travelTimeAdjuster The function adjusting the travel time from this location, it must not be null.
     *        A typical use-case is to accommodate a safe travel-time budget. If {@link LongUnaryOperator#identity()},
     *        this instance is returned as is.
     *
     * @return a new {@link TravelTime} instance with adjusted travel time; if <code>travelTimeAdjuster</code> is
     *         identity, returns this instance; if this {@link TravelTime} is unreachable, returns this instance
     */
    public TravelTime adjust(LongUnaryOperator travelTimeAdjuster) {
        Objects.requireNonNull(travelTimeAdjuster);
        if (travelTimeAdjuster == LongUnaryOperator.identity() || !isReachable()) {
            // to avoid creating a new instance in case of identity, adjusted unreachable travel time is still unreachable
            return this;
        }
        return TravelTime.of(travelTimeAdjuster.applyAsLong(seconds));
    }
}
