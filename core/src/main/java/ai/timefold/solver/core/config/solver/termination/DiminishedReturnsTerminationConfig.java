package ai.timefold.solver.core.config.solver.termination;

import static ai.timefold.solver.core.config.solver.termination.TerminationConfig.requireNonNegative;

import java.time.Duration;
import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import ai.timefold.solver.core.config.AbstractConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.io.jaxb.adapter.JaxbDurationAdapter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "slidingWindowDuration",
        "slidingWindowMilliseconds",
        "slidingWindowSeconds",
        "slidingWindowMinutes",
        "slidingWindowHours",
        "slidingWindowDays",
        "minimumImprovementRatio"
})
public class DiminishedReturnsTerminationConfig extends AbstractConfig<DiminishedReturnsTerminationConfig> {
    @XmlJavaTypeAdapter(JaxbDurationAdapter.class)
    private Duration slidingWindowDuration = null;
    private Long slidingWindowMilliseconds = null;
    private Long slidingWindowSeconds = null;
    private Long slidingWindowMinutes = null;
    private Long slidingWindowHours = null;
    private Long slidingWindowDays = null;

    private Double minimumImprovementRatio = null;

    public @Nullable Duration getSlidingWindowDuration() {
        return slidingWindowDuration;
    }

    public void setSlidingWindowDuration(@Nullable Duration slidingWindowDuration) {
        this.slidingWindowDuration = slidingWindowDuration;
    }

    public @Nullable Long getSlidingWindowMilliseconds() {
        return slidingWindowMilliseconds;
    }

    public void setSlidingWindowMilliseconds(@Nullable Long slidingWindowMilliseconds) {
        this.slidingWindowMilliseconds = slidingWindowMilliseconds;
    }

    public @Nullable Long getSlidingWindowSeconds() {
        return slidingWindowSeconds;
    }

    public void setSlidingWindowSeconds(@Nullable Long slidingWindowSeconds) {
        this.slidingWindowSeconds = slidingWindowSeconds;
    }

    public @Nullable Long getSlidingWindowMinutes() {
        return slidingWindowMinutes;
    }

    public void setSlidingWindowMinutes(@Nullable Long slidingWindowMinutes) {
        this.slidingWindowMinutes = slidingWindowMinutes;
    }

    public @Nullable Long getSlidingWindowHours() {
        return slidingWindowHours;
    }

    public void setSlidingWindowHours(@Nullable Long slidingWindowHours) {
        this.slidingWindowHours = slidingWindowHours;
    }

    public @Nullable Long getSlidingWindowDays() {
        return slidingWindowDays;
    }

    public void setSlidingWindowDays(@Nullable Long slidingWindowDays) {
        this.slidingWindowDays = slidingWindowDays;
    }

    public @Nullable Double getMinimumImprovementRatio() {
        return minimumImprovementRatio;
    }

    public void setMinimumImprovementRatio(@Nullable Double minimumImprovementRatio) {
        this.minimumImprovementRatio = minimumImprovementRatio;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************
    @NonNull
    public DiminishedReturnsTerminationConfig withSlidingWindowDuration(@NonNull Duration slidingWindowDuration) {
        this.slidingWindowDuration = slidingWindowDuration;
        return this;
    }

    @NonNull
    public DiminishedReturnsTerminationConfig withSlidingWindowMilliseconds(@NonNull Long slidingWindowMilliseconds) {
        this.slidingWindowMilliseconds = slidingWindowMilliseconds;
        return this;
    }

    @NonNull
    public DiminishedReturnsTerminationConfig withSlidingWindowSeconds(@NonNull Long slidingWindowSeconds) {
        this.slidingWindowSeconds = slidingWindowSeconds;
        return this;
    }

    @NonNull
    public DiminishedReturnsTerminationConfig withSlidingWindowMinutes(@NonNull Long slidingWindowMinutes) {
        this.slidingWindowMinutes = slidingWindowMinutes;
        return this;
    }

    @NonNull
    public DiminishedReturnsTerminationConfig withSlidingWindowHours(@NonNull Long slidingWindowHours) {
        this.slidingWindowHours = slidingWindowHours;
        return this;
    }

    @NonNull
    public DiminishedReturnsTerminationConfig withSlidingWindowDays(@NonNull Long slidingWindowDays) {
        this.slidingWindowDays = slidingWindowDays;
        return this;
    }

    @NonNull
    public DiminishedReturnsTerminationConfig withMinimumImprovementRatio(@NonNull Double minimumImprovementRatio) {
        this.minimumImprovementRatio = minimumImprovementRatio;
        return this;
    }

    // Complex methods
    @Override
    public @NonNull DiminishedReturnsTerminationConfig inherit(@NonNull DiminishedReturnsTerminationConfig inheritedConfig) {
        if (!slidingWindowIsSet()) {
            inheritSlidingWindow(inheritedConfig);
        }
        minimumImprovementRatio = ConfigUtils.inheritOverwritableProperty(minimumImprovementRatio,
                inheritedConfig.getMinimumImprovementRatio());
        return this;
    }

    private void inheritSlidingWindow(@NonNull DiminishedReturnsTerminationConfig parent) {
        slidingWindowDuration =
                ConfigUtils.inheritOverwritableProperty(slidingWindowDuration, parent.getSlidingWindowDuration());
        slidingWindowMilliseconds = ConfigUtils.inheritOverwritableProperty(slidingWindowMilliseconds,
                parent.getSlidingWindowMilliseconds());
        slidingWindowSeconds = ConfigUtils.inheritOverwritableProperty(slidingWindowSeconds, parent.getSlidingWindowSeconds());
        slidingWindowMinutes = ConfigUtils.inheritOverwritableProperty(slidingWindowMinutes, parent.getSlidingWindowMinutes());
        slidingWindowHours = ConfigUtils.inheritOverwritableProperty(slidingWindowHours, parent.getSlidingWindowHours());
        slidingWindowDays = ConfigUtils.inheritOverwritableProperty(slidingWindowDays, parent.getSlidingWindowDays());
    }

    @Override
    public @NonNull DiminishedReturnsTerminationConfig copyConfig() {
        return new DiminishedReturnsTerminationConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        // intentionally empty - no classes to visit
    }

    /** Check whether any slidingWindow... is non-null. */
    public boolean slidingWindowIsSet() {
        return slidingWindowDuration != null
                || slidingWindowMilliseconds != null
                || slidingWindowSeconds != null
                || slidingWindowMinutes != null
                || slidingWindowHours != null
                || slidingWindowDays != null;
    }

    public @Nullable Long calculateSlidingWindowMilliseconds() {
        if (slidingWindowMilliseconds == null && slidingWindowSeconds == null
                && slidingWindowMinutes == null && slidingWindowHours == null
                && slidingWindowDays == null) {
            if (slidingWindowDuration != null) {
                if (slidingWindowDuration.getNano() % 1000 != 0) {
                    throw new IllegalArgumentException("The termination slidingWindowDuration (" + slidingWindowDuration
                            + ") cannot use nanoseconds.");
                }
                return slidingWindowDuration.toMillis();
            }
            return null;
        }
        if (slidingWindowDuration != null) {
            throw new IllegalArgumentException("The termination slidingWindowDuration (" + slidingWindowDuration
                    + ") cannot be combined with slidingWindowMilliseconds (" + slidingWindowMilliseconds
                    + "), slidingWindowSeconds (" + slidingWindowSeconds
                    + "), slidingWindowMinutes (" + slidingWindowMinutes
                    + "), slidingWindowHours (" + slidingWindowHours + "),"
                    + ") or slidingWindowDays (" + slidingWindowDays + ").");
        }
        long slidingWindowMillis = 0L
                + requireNonNegative(slidingWindowMilliseconds, "slidingWindowMilliseconds")
                + requireNonNegative(slidingWindowSeconds, "slidingWindowSeconds") * 1000L
                + requireNonNegative(slidingWindowMinutes, "slidingWindowMinutes") * 60_000L
                + requireNonNegative(slidingWindowHours, "slidingWindowHours") * 3_600_000L
                + requireNonNegative(slidingWindowDays, "slidingWindowDays") * 86_400_000L;
        return slidingWindowMillis;
    }
}
