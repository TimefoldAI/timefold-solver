package ai.timefold.solver.core.config.localsearch.decider.forager;

import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.AbstractConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "pickEarlyType",
        "acceptedCountLimit",
        "finalistPodiumType",
        "breakTieRandomly"
})
public class LocalSearchForagerConfig extends AbstractConfig<LocalSearchForagerConfig> {

    protected LocalSearchPickEarlyType pickEarlyType = null;
    protected Integer acceptedCountLimit = null;
    protected FinalistPodiumType finalistPodiumType = null;
    protected Boolean breakTieRandomly = null;

    public @Nullable LocalSearchPickEarlyType getPickEarlyType() {
        return pickEarlyType;
    }

    public void setPickEarlyType(@Nullable LocalSearchPickEarlyType pickEarlyType) {
        this.pickEarlyType = pickEarlyType;
    }

    public @Nullable Integer getAcceptedCountLimit() {
        return acceptedCountLimit;
    }

    public void setAcceptedCountLimit(@Nullable Integer acceptedCountLimit) {
        this.acceptedCountLimit = acceptedCountLimit;
    }

    public @Nullable FinalistPodiumType getFinalistPodiumType() {
        return finalistPodiumType;
    }

    public void setFinalistPodiumType(@Nullable FinalistPodiumType finalistPodiumType) {
        this.finalistPodiumType = finalistPodiumType;
    }

    public @Nullable Boolean getBreakTieRandomly() {
        return breakTieRandomly;
    }

    public void setBreakTieRandomly(@Nullable Boolean breakTieRandomly) {
        this.breakTieRandomly = breakTieRandomly;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public @NonNull LocalSearchForagerConfig withPickEarlyType(@NonNull LocalSearchPickEarlyType pickEarlyType) {
        this.pickEarlyType = pickEarlyType;
        return this;
    }

    public @NonNull LocalSearchForagerConfig withAcceptedCountLimit(int acceptedCountLimit) {
        this.acceptedCountLimit = acceptedCountLimit;
        return this;
    }

    public @NonNull LocalSearchForagerConfig withFinalistPodiumType(@NonNull FinalistPodiumType finalistPodiumType) {
        this.finalistPodiumType = finalistPodiumType;
        return this;
    }

    public @NonNull LocalSearchForagerConfig withBreakTieRandomly(boolean breakTieRandomly) {
        this.breakTieRandomly = breakTieRandomly;
        return this;
    }

    @Override
    public @NonNull LocalSearchForagerConfig inherit(@NonNull LocalSearchForagerConfig inheritedConfig) {
        pickEarlyType = ConfigUtils.inheritOverwritableProperty(pickEarlyType,
                inheritedConfig.getPickEarlyType());
        acceptedCountLimit = ConfigUtils.inheritOverwritableProperty(acceptedCountLimit,
                inheritedConfig.getAcceptedCountLimit());
        finalistPodiumType = ConfigUtils.inheritOverwritableProperty(finalistPodiumType,
                inheritedConfig.getFinalistPodiumType());
        breakTieRandomly = ConfigUtils.inheritOverwritableProperty(breakTieRandomly,
                inheritedConfig.getBreakTieRandomly());
        return this;
    }

    @Override
    public @NonNull LocalSearchForagerConfig copyConfig() {
        return new LocalSearchForagerConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        // No referenced classes
    }

}
