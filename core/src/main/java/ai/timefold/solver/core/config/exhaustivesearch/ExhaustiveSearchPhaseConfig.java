package ai.timefold.solver.core.config.exhaustivesearch;

import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySorterManner;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.CartesianProductMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.UnionMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.factory.MoveIteratorFactoryConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.factory.MoveListFactoryConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.ChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.PillarChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.PillarSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.SwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.SubChainChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.SubChainSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.TailChainSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSorterManner;
import ai.timefold.solver.core.config.phase.PhaseConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "exhaustiveSearchType",
        "nodeExplorationType",
        "entitySorterManner",
        "valueSorterManner",
        "entitySelectorConfig",
        "moveSelectorConfig"
})
public class ExhaustiveSearchPhaseConfig extends PhaseConfig<ExhaustiveSearchPhaseConfig> {

    public static final String XML_ELEMENT_NAME = "exhaustiveSearch";

    // Warning: all fields are null (and not defaulted) because they can be inherited
    // and also because the input config file should match the output config file

    protected ExhaustiveSearchType exhaustiveSearchType = null;
    protected NodeExplorationType nodeExplorationType = null;
    protected EntitySorterManner entitySorterManner = null;
    protected ValueSorterManner valueSorterManner = null;

    @XmlElement(name = "entitySelector")
    protected EntitySelectorConfig entitySelectorConfig = null;

    @XmlElements({
            @XmlElement(name = CartesianProductMoveSelectorConfig.XML_ELEMENT_NAME,
                    type = CartesianProductMoveSelectorConfig.class),
            @XmlElement(name = ChangeMoveSelectorConfig.XML_ELEMENT_NAME, type = ChangeMoveSelectorConfig.class),
            @XmlElement(name = MoveIteratorFactoryConfig.XML_ELEMENT_NAME, type = MoveIteratorFactoryConfig.class),
            @XmlElement(name = MoveListFactoryConfig.XML_ELEMENT_NAME, type = MoveListFactoryConfig.class),
            @XmlElement(name = PillarChangeMoveSelectorConfig.XML_ELEMENT_NAME,
                    type = PillarChangeMoveSelectorConfig.class),
            @XmlElement(name = PillarSwapMoveSelectorConfig.XML_ELEMENT_NAME, type = PillarSwapMoveSelectorConfig.class),
            @XmlElement(name = SubChainChangeMoveSelectorConfig.XML_ELEMENT_NAME,
                    type = SubChainChangeMoveSelectorConfig.class),
            @XmlElement(name = SubChainSwapMoveSelectorConfig.XML_ELEMENT_NAME,
                    type = SubChainSwapMoveSelectorConfig.class),
            @XmlElement(name = SwapMoveSelectorConfig.XML_ELEMENT_NAME, type = SwapMoveSelectorConfig.class),
            @XmlElement(name = TailChainSwapMoveSelectorConfig.XML_ELEMENT_NAME,
                    type = TailChainSwapMoveSelectorConfig.class),
            @XmlElement(name = UnionMoveSelectorConfig.XML_ELEMENT_NAME, type = UnionMoveSelectorConfig.class)
    })
    protected MoveSelectorConfig moveSelectorConfig = null;

    public @Nullable ExhaustiveSearchType getExhaustiveSearchType() {
        return exhaustiveSearchType;
    }

    public void setExhaustiveSearchType(@Nullable ExhaustiveSearchType exhaustiveSearchType) {
        this.exhaustiveSearchType = exhaustiveSearchType;
    }

    public @Nullable NodeExplorationType getNodeExplorationType() {
        return nodeExplorationType;
    }

    public void setNodeExplorationType(@Nullable NodeExplorationType nodeExplorationType) {
        this.nodeExplorationType = nodeExplorationType;
    }

    public @Nullable EntitySorterManner getEntitySorterManner() {
        return entitySorterManner;
    }

    public void setEntitySorterManner(@Nullable EntitySorterManner entitySorterManner) {
        this.entitySorterManner = entitySorterManner;
    }

    public @Nullable ValueSorterManner getValueSorterManner() {
        return valueSorterManner;
    }

    public void setValueSorterManner(@Nullable ValueSorterManner valueSorterManner) {
        this.valueSorterManner = valueSorterManner;
    }

    public @Nullable EntitySelectorConfig getEntitySelectorConfig() {
        return entitySelectorConfig;
    }

    public void setEntitySelectorConfig(@Nullable EntitySelectorConfig entitySelectorConfig) {
        this.entitySelectorConfig = entitySelectorConfig;
    }

    public @Nullable MoveSelectorConfig getMoveSelectorConfig() {
        return moveSelectorConfig;
    }

    public void setMoveSelectorConfig(@Nullable MoveSelectorConfig moveSelectorConfig) {
        this.moveSelectorConfig = moveSelectorConfig;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public @NonNull ExhaustiveSearchPhaseConfig withExhaustiveSearchType(@NonNull ExhaustiveSearchType exhaustiveSearchType) {
        this.setExhaustiveSearchType(exhaustiveSearchType);
        return this;
    }

    public @NonNull ExhaustiveSearchPhaseConfig withNodeExplorationType(@NonNull NodeExplorationType nodeExplorationType) {
        this.setNodeExplorationType(nodeExplorationType);
        return this;
    }

    public @NonNull ExhaustiveSearchPhaseConfig withEntitySorterManner(@NonNull EntitySorterManner entitySorterManner) {
        this.setEntitySorterManner(entitySorterManner);
        return this;
    }

    public @NonNull ExhaustiveSearchPhaseConfig withValueSorterManner(@NonNull ValueSorterManner valueSorterManner) {
        this.setValueSorterManner(valueSorterManner);
        return this;
    }

    public @NonNull ExhaustiveSearchPhaseConfig withEntitySelectorConfig(@NonNull EntitySelectorConfig entitySelectorConfig) {
        this.setEntitySelectorConfig(entitySelectorConfig);
        return this;
    }

    public @NonNull ExhaustiveSearchPhaseConfig withMoveSelectorConfig(@NonNull MoveSelectorConfig moveSelectorConfig) {
        this.setMoveSelectorConfig(moveSelectorConfig);
        return this;
    }

    @Override
    public @NonNull ExhaustiveSearchPhaseConfig inherit(@NonNull ExhaustiveSearchPhaseConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        exhaustiveSearchType = ConfigUtils.inheritOverwritableProperty(exhaustiveSearchType,
                inheritedConfig.getExhaustiveSearchType());
        nodeExplorationType = ConfigUtils.inheritOverwritableProperty(nodeExplorationType,
                inheritedConfig.getNodeExplorationType());
        entitySorterManner = ConfigUtils.inheritOverwritableProperty(entitySorterManner,
                inheritedConfig.getEntitySorterManner());
        valueSorterManner = ConfigUtils.inheritOverwritableProperty(valueSorterManner,
                inheritedConfig.getValueSorterManner());
        entitySelectorConfig = ConfigUtils.inheritConfig(entitySelectorConfig, inheritedConfig.getEntitySelectorConfig());
        moveSelectorConfig = ConfigUtils.inheritConfig(moveSelectorConfig, inheritedConfig.getMoveSelectorConfig());
        return this;
    }

    @Override
    public @NonNull ExhaustiveSearchPhaseConfig copyConfig() {
        return new ExhaustiveSearchPhaseConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        if (terminationConfig != null) {
            terminationConfig.visitReferencedClasses(classVisitor);
        }
        if (entitySelectorConfig != null) {
            entitySelectorConfig.visitReferencedClasses(classVisitor);
        }
        if (moveSelectorConfig != null) {
            moveSelectorConfig.visitReferencedClasses(classVisitor);
        }
    }

}
