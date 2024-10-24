package ai.timefold.solver.core.config.localsearch.decider.acceptor;

import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.AbstractConfig;

@XmlType(propOrder = {
        "moveCountLimitPercentage",
        "lateAcceptanceReconfigurationSize"
})
public class ReconfigurationConfig extends AbstractConfig<ReconfigurationConfig> {

    private Double moveCountLimitPercentage = null;
    private Long lateAcceptanceReconfigurationSize = null;

    public Double getMoveCountLimitPercentage() {
        return moveCountLimitPercentage;
    }

    public void setMoveCountLimitPercentage(Double moveCountLimitPercentage) {
        this.moveCountLimitPercentage = moveCountLimitPercentage;
    }

    public Long getLateAcceptanceReconfigurationSize() {
        return lateAcceptanceReconfigurationSize;
    }

    public void setLateAcceptanceReconfigurationSize(Long lateAcceptanceReconfigurationSize) {
        this.lateAcceptanceReconfigurationSize = lateAcceptanceReconfigurationSize;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public ReconfigurationConfig withMoveCountLimitPercentage(Double moveCountLimitPercentage) {
        this.moveCountLimitPercentage = moveCountLimitPercentage;
        return this;
    }

    public ReconfigurationConfig withReconfigurationRatio(Long reconfigurationRatio) {
        this.lateAcceptanceReconfigurationSize = reconfigurationRatio;
        return this;
    }

    @Override
    public ReconfigurationConfig inherit(ReconfigurationConfig inheritedConfig) {
        moveCountLimitPercentage = inheritedConfig.getMoveCountLimitPercentage();
        lateAcceptanceReconfigurationSize = inheritedConfig.getLateAcceptanceReconfigurationSize();
        return this;
    }

    @Override
    public ReconfigurationConfig copyConfig() {
        return new ReconfigurationConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(Consumer<Class<?>> classVisitor) {
        // No referenced classes
    }
}
