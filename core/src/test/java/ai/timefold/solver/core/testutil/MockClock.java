package ai.timefold.solver.core.testutil;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

public final class MockClock extends Clock {

    private final ZoneId zone;
    private Instant currentInstant;

    public MockClock(Clock clock) {
        this(clock.instant(), clock.getZone());
    }

    private MockClock(Instant instant, ZoneId zoneId) {
        this.zone = Objects.requireNonNull(zoneId);
        this.currentInstant = Objects.requireNonNull(instant);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MockClock(currentInstant, zone);
    }

    @Override
    public Instant instant() {
        return currentInstant;
    }

    public void tick(Duration duration) {
        currentInstant = currentInstant.plus(duration);
    }

}
