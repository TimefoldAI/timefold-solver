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
        "gracePeriod",
        "gracePeriodMilliseconds",
        "gracePeriodSeconds",
        "gracePeriodMinutes",
        "gracePeriodHours",
        "gracePeriodDays",
        "minimumImprovementRatio"
})
public class AdaptiveTerminationConfig extends AbstractConfig<AdaptiveTerminationConfig> {
    @XmlJavaTypeAdapter(JaxbDurationAdapter.class)
    private Duration gracePeriod = null;
    private Long gracePeriodMilliseconds = null;
    private Long gracePeriodSeconds = null;
    private Long gracePeriodMinutes = null;
    private Long gracePeriodHours = null;
    private Long gracePeriodDays = null;

    private Double minimumImprovementRatio = null;

    public @Nullable Duration getGracePeriod() {
        return gracePeriod;
    }

    public void setGracePeriod(@Nullable Duration gracePeriod) {
        this.gracePeriod = gracePeriod;
    }

    public @Nullable Long getGracePeriodMilliseconds() {
        return gracePeriodMilliseconds;
    }

    public void setGracePeriodMilliseconds(@Nullable Long gracePeriodMilliseconds) {
        this.gracePeriodMilliseconds = gracePeriodMilliseconds;
    }

    public @Nullable Long getGracePeriodSeconds() {
        return gracePeriodSeconds;
    }

    public void setGracePeriodSeconds(@Nullable Long gracePeriodSeconds) {
        this.gracePeriodSeconds = gracePeriodSeconds;
    }

    public @Nullable Long getGracePeriodMinutes() {
        return gracePeriodMinutes;
    }

    public void setGracePeriodMinutes(@Nullable Long gracePeriodMinutes) {
        this.gracePeriodMinutes = gracePeriodMinutes;
    }

    public @Nullable Long getGracePeriodHours() {
        return gracePeriodHours;
    }

    public void setGracePeriodHours(@Nullable Long gracePeriodHours) {
        this.gracePeriodHours = gracePeriodHours;
    }

    public @Nullable Long getGracePeriodDays() {
        return gracePeriodDays;
    }

    public void setGracePeriodDays(@Nullable Long gracePeriodDays) {
        this.gracePeriodDays = gracePeriodDays;
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
    public AdaptiveTerminationConfig withGracePeriod(@NonNull Duration gracePeriod) {
        this.gracePeriod = gracePeriod;
        return this;
    }

    @NonNull
    public AdaptiveTerminationConfig withGracePeriodMilliseconds(@NonNull Long gracePeriodMilliseconds) {
        this.gracePeriodMilliseconds = gracePeriodMilliseconds;
        return this;
    }

    @NonNull
    public AdaptiveTerminationConfig withGracePeriodSeconds(@NonNull Long gracePeriodSeconds) {
        this.gracePeriodSeconds = gracePeriodSeconds;
        return this;
    }

    @NonNull
    public AdaptiveTerminationConfig withGracePeriodMinutes(@NonNull Long gracePeriodMinutes) {
        this.gracePeriodMinutes = gracePeriodMinutes;
        return this;
    }

    @NonNull
    public AdaptiveTerminationConfig withGracePeriodHours(@NonNull Long gracePeriodHours) {
        this.gracePeriodHours = gracePeriodHours;
        return this;
    }

    @NonNull
    public AdaptiveTerminationConfig withGracePeriodDays(@NonNull Long gracePeriodDays) {
        this.gracePeriodDays = gracePeriodDays;
        return this;
    }

    @NonNull
    public AdaptiveTerminationConfig withMinimumImprovementRatio(@NonNull Double minimumImprovementRatio) {
        this.minimumImprovementRatio = minimumImprovementRatio;
        return this;
    }

    // Complex methods
    @Override
    public @NonNull AdaptiveTerminationConfig inherit(@NonNull AdaptiveTerminationConfig inheritedConfig) {
        if (!gracePeriodIsSet()) {
            inheritGracePeriod(inheritedConfig);
        }
        minimumImprovementRatio = ConfigUtils.inheritOverwritableProperty(minimumImprovementRatio,
                inheritedConfig.getMinimumImprovementRatio());
        return this;
    }

    private void inheritGracePeriod(@NonNull AdaptiveTerminationConfig parent) {
        gracePeriod = ConfigUtils.inheritOverwritableProperty(gracePeriod, parent.getGracePeriod());
        gracePeriodMilliseconds = ConfigUtils.inheritOverwritableProperty(gracePeriodMilliseconds,
                parent.getGracePeriodMilliseconds());
        gracePeriodSeconds = ConfigUtils.inheritOverwritableProperty(gracePeriodSeconds, parent.getGracePeriodSeconds());
        gracePeriodMinutes = ConfigUtils.inheritOverwritableProperty(gracePeriodMinutes, parent.getGracePeriodMinutes());
        gracePeriodHours = ConfigUtils.inheritOverwritableProperty(gracePeriodHours, parent.getGracePeriodHours());
        gracePeriodDays = ConfigUtils.inheritOverwritableProperty(gracePeriodDays, parent.getGracePeriodDays());
    }

    @Override
    public @NonNull AdaptiveTerminationConfig copyConfig() {
        return new AdaptiveTerminationConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {

    }

    /** Check whether any gracePeriod... is non-null. */
    public boolean gracePeriodIsSet() {
        return gracePeriod != null
                || gracePeriodMilliseconds != null
                || gracePeriodSeconds != null
                || gracePeriodMinutes != null
                || gracePeriodHours != null
                || gracePeriodDays != null;
    }

    public @Nullable Long calculateGracePeriodMilliseconds() {
        if (gracePeriodMilliseconds == null && gracePeriodSeconds == null
                && gracePeriodMinutes == null && gracePeriodHours == null
                && gracePeriodDays == null) {
            if (gracePeriod != null) {
                if (gracePeriod.getNano() % 1000 != 0) {
                    throw new IllegalArgumentException("The termination gracePeriod (" + gracePeriod
                            + ") cannot use nanoseconds.");
                }
                return gracePeriod.toMillis();
            }
            return null;
        }
        if (gracePeriod != null) {
            throw new IllegalArgumentException("The termination gracePeriod (" + gracePeriod
                    + ") cannot be combined with gracePeriodMilliseconds (" + gracePeriodMilliseconds
                    + "), gracePeriodSeconds (" + gracePeriodSeconds
                    + "), gracePeriodMinutes (" + gracePeriodMinutes
                    + "), gracePeriodHours (" + gracePeriodHours + "),"
                    + ") or gracePeriodDays (" + gracePeriodDays + ").");
        }
        long gracePeriodMillis = 0L
                + requireNonNegative(gracePeriodMilliseconds, "gracePeriodMilliseconds")
                + requireNonNegative(gracePeriodSeconds, "gracePeriodSeconds") * 1000L
                + requireNonNegative(gracePeriodMinutes, "gracePeriodMinutes") * 60_000L
                + requireNonNegative(gracePeriodHours, "gracePeriodHours") * 3_600_000L
                + requireNonNegative(gracePeriodDays, "gracePeriodDays") * 86_400_000L;
        return gracePeriodMillis;
    }
}
