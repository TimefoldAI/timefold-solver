package ai.timefold.solver.core.config.localsearch;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import ai.timefold.solver.core.config.AbstractConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.solver.NeighborhoodsConfig;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.io.jaxb.JaxbCustomPropertiesAdapter;
import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodProvider;
import ai.timefold.solver.core.preview.api.neighborhood.selection.MoveSelectionStrategy;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The Neighborhoods API settings of one Local Search phase.
 * Each setting overrides the same setting of the solver-level {@link NeighborhoodsConfig};
 * the strategy class and its custom properties always go together.
 * <p>
 * Part of {@link PreviewFeature#NEIGHBORHOODS}.
 */
@XmlType(name = "localSearchNeighborhoodsConfig", propOrder = {
        "neighborhoodProviderClass",
        "moveSelectionStrategyClass",
        "moveSelectionStrategyCustomProperties",
})
public final class LocalSearchNeighborhoodsConfig extends AbstractConfig<LocalSearchNeighborhoodsConfig> {

    public static final String XML_ELEMENT_NAME = "neighborhoods";

    private String neighborhoodProviderClass = null;
    private String moveSelectionStrategyClass = null;
    @XmlJavaTypeAdapter(JaxbCustomPropertiesAdapter.class)
    private Map<String, String> moveSelectionStrategyCustomProperties = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    /**
     * If the phase has no {@link MoveSelectorConfig}, the solver uses only the moves of the neighborhood.
     * Otherwise it uses the moves of both.
     */
    @SuppressWarnings("unchecked")
    public @Nullable <Solution_> Class<? extends NeighborhoodProvider<Solution_>> getNeighborhoodProviderClass() {
        return ConfigUtils.resolveClass(neighborhoodProviderClass, "neighborhoodProviderClass", this);
    }

    @SuppressWarnings("rawtypes")
    public void setNeighborhoodProviderClass(@Nullable Class<? extends NeighborhoodProvider> neighborhoodProviderClass) {
        this.neighborhoodProviderClass = neighborhoodProviderClass == null ? null : neighborhoodProviderClass.getName();
    }

    /**
     * Not allowed in a phase with a {@link MoveSelectorConfig}.
     */
    @SuppressWarnings("unchecked")
    public @Nullable <Solution_> Class<? extends MoveSelectionStrategy<Solution_>> getMoveSelectionStrategyClass() {
        return ConfigUtils.resolveClass(moveSelectionStrategyClass, "moveSelectionStrategyClass", this);
    }

    @SuppressWarnings("rawtypes")
    public void setMoveSelectionStrategyClass(@Nullable Class<? extends MoveSelectionStrategy> moveSelectionStrategyClass) {
        this.moveSelectionStrategyClass = moveSelectionStrategyClass == null ? null : moveSelectionStrategyClass.getName();
    }

    /**
     * Applied to the {@link #getMoveSelectionStrategyClass()} of this phase only.
     */
    public @Nullable Map<@NonNull String, @NonNull String> getMoveSelectionStrategyCustomProperties() {
        return moveSelectionStrategyCustomProperties;
    }

    public void setMoveSelectionStrategyCustomProperties(
            @Nullable Map<@NonNull String, @NonNull String> moveSelectionStrategyCustomProperties) {
        this.moveSelectionStrategyCustomProperties = moveSelectionStrategyCustomProperties;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public @NonNull LocalSearchNeighborhoodsConfig
            withNeighborhoodProviderClass(@NonNull Class<? extends NeighborhoodProvider<?>> neighborhoodProviderClass) {
        this.neighborhoodProviderClass = neighborhoodProviderClass.getName();
        return this;
    }

    public @NonNull LocalSearchNeighborhoodsConfig withMoveSelectionStrategyClass(
            @NonNull Class<? extends MoveSelectionStrategy<?>> moveSelectionStrategyClass) {
        this.moveSelectionStrategyClass = moveSelectionStrategyClass.getName();
        return this;
    }

    public @NonNull LocalSearchNeighborhoodsConfig withMoveSelectionStrategyCustomProperties(
            @NonNull Map<@NonNull String, @NonNull String> moveSelectionStrategyCustomProperties) {
        this.moveSelectionStrategyCustomProperties = moveSelectionStrategyCustomProperties;
        return this;
    }

    @Override
    public @NonNull LocalSearchNeighborhoodsConfig inherit(@NonNull LocalSearchNeighborhoodsConfig inheritedConfig) {
        neighborhoodProviderClass = ConfigUtils.inheritOverwritableProperty(neighborhoodProviderClass,
                inheritedConfig.neighborhoodProviderClass);
        // The class and the properties always go together.
        if (moveSelectionStrategyClass == null
                || Objects.equals(moveSelectionStrategyClass, inheritedConfig.moveSelectionStrategyClass)) {
            moveSelectionStrategyClass = inheritedConfig.moveSelectionStrategyClass;
            moveSelectionStrategyCustomProperties = ConfigUtils.inheritMergeableMapProperty(
                    moveSelectionStrategyCustomProperties, inheritedConfig.moveSelectionStrategyCustomProperties);
        }
        return this;
    }

    @Override
    public @NonNull LocalSearchNeighborhoodsConfig copyConfig() {
        return new LocalSearchNeighborhoodsConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        if (neighborhoodProviderClass != null) {
            classVisitor.accept(getNeighborhoodProviderClass());
        }
        if (moveSelectionStrategyClass != null) {
            classVisitor.accept(getMoveSelectionStrategyClass());
        }
    }

}
