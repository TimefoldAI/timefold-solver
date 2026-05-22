package ai.timefold.solver.core.config.heuristic.selector.move.generic;

import java.util.Comparator;
import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.heuristic.selector.entity.pillar.PillarSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "subPillarType",
        "subPillarSequenceComparatorClass",
        "pillarSelectorConfig"
})
public abstract class AbstractPillarMoveSelectorConfig<Config_ extends AbstractPillarMoveSelectorConfig<Config_>>
        extends MoveSelectorConfig<Config_> {

    protected SubPillarType subPillarType = null;
    protected String subPillarSequenceComparatorClass = null;
    @XmlElement(name = "pillarSelector")
    protected PillarSelectorConfig pillarSelectorConfig = null;

    public @Nullable SubPillarType getSubPillarType() {
        return subPillarType;
    }

    public void setSubPillarType(final @Nullable SubPillarType subPillarType) {
        this.subPillarType = subPillarType;
    }

    public @Nullable Class<? extends Comparator> getSubPillarSequenceComparatorClass() {
        return ConfigUtils.resolveClass(subPillarSequenceComparatorClass, "subPillarSequenceComparatorClass", this);
    }

    public void
            setSubPillarSequenceComparatorClass(final @Nullable Class<? extends Comparator> subPillarSequenceComparatorClass) {
        this.subPillarSequenceComparatorClass =
                subPillarSequenceComparatorClass == null ? null : subPillarSequenceComparatorClass.getName();
    }

    public @Nullable PillarSelectorConfig getPillarSelectorConfig() {
        return pillarSelectorConfig;
    }

    public void setPillarSelectorConfig(@Nullable PillarSelectorConfig pillarSelectorConfig) {
        this.pillarSelectorConfig = pillarSelectorConfig;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public @NonNull Config_ withSubPillarType(@NonNull SubPillarType subPillarType) {
        this.setSubPillarType(subPillarType);
        return (Config_) this;
    }

    public @NonNull Config_
            withSubPillarSequenceComparatorClass(@NonNull Class<? extends Comparator> subPillarSequenceComparatorClass) {
        this.subPillarSequenceComparatorClass = subPillarSequenceComparatorClass.getName();
        return (Config_) this;
    }

    public @NonNull Config_ withPillarSelectorConfig(@NonNull PillarSelectorConfig pillarSelectorConfig) {
        this.setPillarSelectorConfig(pillarSelectorConfig);
        return (Config_) this;
    }

    @Override
    public @NonNull Config_ inherit(@NonNull Config_ inheritedConfig) {
        super.inherit(inheritedConfig);
        subPillarType = ConfigUtils.inheritOverwritableProperty(subPillarType, inheritedConfig.getSubPillarType());
        subPillarSequenceComparatorClass = ConfigUtils.inheritOverwritableProperty(subPillarSequenceComparatorClass,
                inheritedConfig.subPillarSequenceComparatorClass);
        pillarSelectorConfig = ConfigUtils.inheritConfig(pillarSelectorConfig, inheritedConfig.getPillarSelectorConfig());
        return (Config_) this;
    }

    @Override
    protected void visitCommonReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        super.visitCommonReferencedClasses(classVisitor);
        classVisitor.accept(getSubPillarSequenceComparatorClass());
        if (pillarSelectorConfig != null) {
            pillarSelectorConfig.visitReferencedClasses(classVisitor);
        }
    }

    @Override
    public boolean hasNearbySelectionConfig() {
        return pillarSelectorConfig != null && pillarSelectorConfig.hasNearbySelectionConfig();
    }
}
