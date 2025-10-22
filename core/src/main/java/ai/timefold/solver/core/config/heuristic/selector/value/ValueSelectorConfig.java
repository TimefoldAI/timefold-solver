package ai.timefold.solver.core.config.heuristic.selector.value;

import java.util.Comparator;
import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.api.domain.common.ComparatorFactory;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.config.heuristic.selector.SelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.common.decorator.SelectionSorterOrder;
import ai.timefold.solver.core.config.heuristic.selector.common.nearby.NearbySelectionConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionProbabilityWeightFactory;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "id",
        "mimicSelectorRef",
        "downcastEntityClass",
        "variableName",
        "cacheType",
        "selectionOrder",
        "nearbySelectionConfig",
        "filterClass",
        "sorterManner",
        "sorterComparatorClass",
        "comparatorClass",
        "sorterWeightFactoryClass",
        "comparatorFactoryClass",
        "sorterOrder",
        "sorterClass",
        "probabilityWeightFactoryClass",
        "selectedCountLimit"
})
public class ValueSelectorConfig extends SelectorConfig<ValueSelectorConfig> {

    @XmlAttribute
    protected String id = null;
    @XmlAttribute
    protected String mimicSelectorRef = null;

    protected Class<?> downcastEntityClass = null;
    @XmlAttribute
    protected String variableName = null;

    protected SelectionCacheType cacheType = null;
    protected SelectionOrder selectionOrder = null;

    @XmlElement(name = "nearbySelection")
    protected NearbySelectionConfig nearbySelectionConfig = null;

    protected Class<? extends SelectionFilter> filterClass = null;

    protected ValueSorterManner sorterManner = null;
    /**
     * @deprecated Deprecated in favor of {@link #comparatorClass}.
     */
    @Deprecated(forRemoval = true, since = "1.28.0")
    protected Class<? extends Comparator> sorterComparatorClass = null;
    protected Class<? extends Comparator> comparatorClass = null;
    /**
     * @deprecated Deprecated in favor of {@link #comparatorFactoryClass}.
     */
    @Deprecated(forRemoval = true, since = "1.28.0")
    protected Class<? extends ComparatorFactory> sorterWeightFactoryClass = null;
    protected Class<? extends ComparatorFactory> comparatorFactoryClass = null;
    protected SelectionSorterOrder sorterOrder = null;
    protected Class<? extends SelectionSorter> sorterClass = null;

    protected Class<? extends SelectionProbabilityWeightFactory> probabilityWeightFactoryClass = null;

    protected Long selectedCountLimit = null;

    public ValueSelectorConfig() {
    }

    public ValueSelectorConfig(@NonNull String variableName) {
        this.variableName = variableName;
    }

    public ValueSelectorConfig(@Nullable ValueSelectorConfig inheritedConfig) {
        if (inheritedConfig != null) {
            inherit(inheritedConfig);
        }
    }

    public @Nullable String getId() {
        return id;
    }

    public void setId(@Nullable String id) {
        this.id = id;
    }

    public @Nullable String getMimicSelectorRef() {
        return mimicSelectorRef;
    }

    public void setMimicSelectorRef(@Nullable String mimicSelectorRef) {
        this.mimicSelectorRef = mimicSelectorRef;
    }

    public @Nullable Class<?> getDowncastEntityClass() {
        return downcastEntityClass;
    }

    public void setDowncastEntityClass(@Nullable Class<?> downcastEntityClass) {
        this.downcastEntityClass = downcastEntityClass;
    }

    public @Nullable String getVariableName() {
        return variableName;
    }

    public void setVariableName(@Nullable String variableName) {
        this.variableName = variableName;
    }

    public @Nullable SelectionCacheType getCacheType() {
        return cacheType;
    }

    public void setCacheType(@Nullable SelectionCacheType cacheType) {
        this.cacheType = cacheType;
    }

    public @Nullable SelectionOrder getSelectionOrder() {
        return selectionOrder;
    }

    public void setSelectionOrder(@Nullable SelectionOrder selectionOrder) {
        this.selectionOrder = selectionOrder;
    }

    public @Nullable NearbySelectionConfig getNearbySelectionConfig() {
        return nearbySelectionConfig;
    }

    public void setNearbySelectionConfig(@Nullable NearbySelectionConfig nearbySelectionConfig) {
        this.nearbySelectionConfig = nearbySelectionConfig;
    }

    public @Nullable Class<? extends SelectionFilter> getFilterClass() {
        return filterClass;
    }

    public void setFilterClass(@Nullable Class<? extends SelectionFilter> filterClass) {
        this.filterClass = filterClass;
    }

    public @Nullable ValueSorterManner getSorterManner() {
        return sorterManner;
    }

    public void setSorterManner(@Nullable ValueSorterManner sorterManner) {
        this.sorterManner = sorterManner;
    }

    /**
     * @deprecated Deprecated in favor of {@link #getComparatorClass()}
     */
    @Deprecated(forRemoval = true, since = "1.28.0")
    public @Nullable Class<? extends Comparator> getSorterComparatorClass() {
        return sorterComparatorClass;
    }

    /**
     * @deprecated Deprecated in favor of {@link #setComparatorClass(Class)}
     */
    @Deprecated(forRemoval = true, since = "1.28.0")
    public void setSorterComparatorClass(@Nullable Class<? extends Comparator> sorterComparatorClass) {
        this.sorterComparatorClass = sorterComparatorClass;
    }

    public Class<? extends Comparator> getComparatorClass() {
        return comparatorClass;
    }

    public void setComparatorClass(Class<? extends Comparator> comparatorClass) {
        this.comparatorClass = comparatorClass;
    }

    /**
     * @deprecated Deprecated in favor of {@link #getComparatorFactoryClass()}
     */
    @Deprecated(forRemoval = true, since = "1.28.0")
    public @Nullable Class<? extends ComparatorFactory> getSorterWeightFactoryClass() {
        return sorterWeightFactoryClass;
    }

    /**
     * @deprecated Deprecated in favor of {@link #setComparatorFactoryClass(Class)}
     * @param sorterWeightFactoryClass the class
     */
    @Deprecated(forRemoval = true, since = "1.28.0")
    public void setSorterWeightFactoryClass(@Nullable Class<? extends ComparatorFactory> sorterWeightFactoryClass) {
        this.sorterWeightFactoryClass = sorterWeightFactoryClass;
    }

    public Class<? extends ComparatorFactory> getComparatorFactoryClass() {
        return comparatorFactoryClass;
    }

    public void setComparatorFactoryClass(Class<? extends ComparatorFactory> comparatorFactoryClass) {
        this.comparatorFactoryClass = comparatorFactoryClass;
    }

    public @Nullable SelectionSorterOrder getSorterOrder() {
        return sorterOrder;
    }

    public void setSorterOrder(@Nullable SelectionSorterOrder sorterOrder) {
        this.sorterOrder = sorterOrder;
    }

    public @Nullable Class<? extends SelectionSorter> getSorterClass() {
        return sorterClass;
    }

    public void setSorterClass(@Nullable Class<? extends SelectionSorter> sorterClass) {
        this.sorterClass = sorterClass;
    }

    public @Nullable Class<? extends SelectionProbabilityWeightFactory> getProbabilityWeightFactoryClass() {
        return probabilityWeightFactoryClass;
    }

    public void setProbabilityWeightFactoryClass(
            @Nullable Class<? extends SelectionProbabilityWeightFactory> probabilityWeightFactoryClass) {
        this.probabilityWeightFactoryClass = probabilityWeightFactoryClass;
    }

    public @Nullable Long getSelectedCountLimit() {
        return selectedCountLimit;
    }

    public void setSelectedCountLimit(@Nullable Long selectedCountLimit) {
        this.selectedCountLimit = selectedCountLimit;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public @NonNull ValueSelectorConfig withId(@NonNull String id) {
        this.setId(id);
        return this;
    }

    public @NonNull ValueSelectorConfig withMimicSelectorRef(@NonNull String mimicSelectorRef) {
        this.setMimicSelectorRef(mimicSelectorRef);
        return this;
    }

    public @NonNull ValueSelectorConfig withDowncastEntityClass(@NonNull Class<?> entityClass) {
        this.setDowncastEntityClass(entityClass);
        return this;
    }

    public @NonNull ValueSelectorConfig withVariableName(@NonNull String variableName) {
        this.setVariableName(variableName);
        return this;
    }

    public @NonNull ValueSelectorConfig withCacheType(@NonNull SelectionCacheType cacheType) {
        this.setCacheType(cacheType);
        return this;
    }

    public @NonNull ValueSelectorConfig withSelectionOrder(@NonNull SelectionOrder selectionOrder) {
        this.setSelectionOrder(selectionOrder);
        return this;
    }

    public @NonNull ValueSelectorConfig withNearbySelectionConfig(@NonNull NearbySelectionConfig nearbySelectionConfig) {
        this.setNearbySelectionConfig(nearbySelectionConfig);
        return this;
    }

    public @NonNull ValueSelectorConfig withFilterClass(@NonNull Class<? extends SelectionFilter> filterClass) {
        this.setFilterClass(filterClass);
        return this;
    }

    public @NonNull ValueSelectorConfig withSorterManner(@NonNull ValueSorterManner sorterManner) {
        this.setSorterManner(sorterManner);
        return this;
    }

    /**
     * @deprecated Deprecated in favor of {@link #withComparatorClass(Class)}
     */
    @Deprecated(forRemoval = true, since = "1.28.0")
    public @NonNull ValueSelectorConfig withSorterComparatorClass(@NonNull Class<? extends Comparator> comparatorClass) {
        this.setSorterComparatorClass(comparatorClass);
        return this;
    }

    public @NonNull ValueSelectorConfig withComparatorClass(@NonNull Class<? extends Comparator> comparatorClass) {
        this.setComparatorClass(comparatorClass);
        return this;
    }

    /**
     * @deprecated Deprecated in favor of {@link #withComparatorFactoryClass(Class)}
     * @param weightFactoryClass the factory class
     */
    @Deprecated(forRemoval = true, since = "1.28.0")
    public @NonNull ValueSelectorConfig
            withSorterWeightFactoryClass(@NonNull Class<? extends ComparatorFactory> weightFactoryClass) {
        this.setSorterWeightFactoryClass(weightFactoryClass);
        return this;
    }

    public @NonNull ValueSelectorConfig
            withComparatorFactoryClass(@NonNull Class<? extends ComparatorFactory> comparatorFactoryClass) {
        this.setComparatorFactoryClass(comparatorFactoryClass);
        return this;
    }

    public @NonNull ValueSelectorConfig withSorterOrder(@NonNull SelectionSorterOrder sorterOrder) {
        this.setSorterOrder(sorterOrder);
        return this;
    }

    public @NonNull ValueSelectorConfig withSorterClass(@NonNull Class<? extends SelectionSorter> sorterClass) {
        this.setSorterClass(sorterClass);
        return this;
    }

    public @NonNull ValueSelectorConfig
            withProbabilityWeightFactoryClass(@NonNull Class<? extends SelectionProbabilityWeightFactory> factoryClass) {
        this.setProbabilityWeightFactoryClass(factoryClass);
        return this;
    }

    public @NonNull ValueSelectorConfig withSelectedCountLimit(long selectedCountLimit) {
        this.setSelectedCountLimit(selectedCountLimit);
        return this;
    }

    // ************************************************************************
    // Builder methods
    // ************************************************************************

    @Override
    public @NonNull ValueSelectorConfig inherit(@NonNull ValueSelectorConfig inheritedConfig) {
        id = ConfigUtils.inheritOverwritableProperty(id, inheritedConfig.getId());
        mimicSelectorRef = ConfigUtils.inheritOverwritableProperty(mimicSelectorRef,
                inheritedConfig.getMimicSelectorRef());
        downcastEntityClass = ConfigUtils.inheritOverwritableProperty(downcastEntityClass,
                inheritedConfig.getDowncastEntityClass());
        variableName = ConfigUtils.inheritOverwritableProperty(variableName, inheritedConfig.getVariableName());
        nearbySelectionConfig = ConfigUtils.inheritConfig(nearbySelectionConfig, inheritedConfig.getNearbySelectionConfig());
        filterClass = ConfigUtils.inheritOverwritableProperty(filterClass, inheritedConfig.getFilterClass());
        cacheType = ConfigUtils.inheritOverwritableProperty(cacheType, inheritedConfig.getCacheType());
        selectionOrder = ConfigUtils.inheritOverwritableProperty(selectionOrder, inheritedConfig.getSelectionOrder());
        sorterManner = ConfigUtils.inheritOverwritableProperty(
                sorterManner, inheritedConfig.getSorterManner());
        sorterComparatorClass = ConfigUtils.inheritOverwritableProperty(
                sorterComparatorClass, inheritedConfig.getSorterComparatorClass());
        comparatorClass = ConfigUtils.inheritOverwritableProperty(
                comparatorClass, inheritedConfig.getComparatorClass());
        sorterWeightFactoryClass = ConfigUtils.inheritOverwritableProperty(
                sorterWeightFactoryClass, inheritedConfig.getSorterWeightFactoryClass());
        comparatorFactoryClass = ConfigUtils.inheritOverwritableProperty(
                comparatorFactoryClass, inheritedConfig.getComparatorFactoryClass());
        sorterOrder = ConfigUtils.inheritOverwritableProperty(
                sorterOrder, inheritedConfig.getSorterOrder());
        sorterClass = ConfigUtils.inheritOverwritableProperty(
                sorterClass, inheritedConfig.getSorterClass());
        probabilityWeightFactoryClass = ConfigUtils.inheritOverwritableProperty(
                probabilityWeightFactoryClass, inheritedConfig.getProbabilityWeightFactoryClass());
        selectedCountLimit = ConfigUtils.inheritOverwritableProperty(
                selectedCountLimit, inheritedConfig.getSelectedCountLimit());
        return this;
    }

    @Override
    public @NonNull ValueSelectorConfig copyConfig() {
        return new ValueSelectorConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        classVisitor.accept(downcastEntityClass);
        if (nearbySelectionConfig != null) {
            nearbySelectionConfig.visitReferencedClasses(classVisitor);
        }
        classVisitor.accept(filterClass);
        classVisitor.accept(sorterComparatorClass);
        classVisitor.accept(comparatorClass);
        classVisitor.accept(sorterWeightFactoryClass);
        classVisitor.accept(comparatorFactoryClass);
        classVisitor.accept(sorterClass);
        classVisitor.accept(probabilityWeightFactoryClass);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + variableName + ")";
    }

    public static <Solution_> boolean hasSorter(@NonNull ValueSorterManner valueSorterManner,
            @NonNull GenuineVariableDescriptor<Solution_> variableDescriptor) {
        return switch (valueSorterManner) {
            case NONE -> false;
            case INCREASING_STRENGTH, DECREASING_STRENGTH, ASCENDING, DESCENDING -> true;
            case INCREASING_STRENGTH_IF_AVAILABLE, ASCENDING_IF_AVAILABLE ->
                variableDescriptor.getAscendingSorter() != null;
            case DECREASING_STRENGTH_IF_AVAILABLE, DESCENDING_IF_AVAILABLE ->
                variableDescriptor.getDescendingSorter() != null;
        };
    }

    public static <Solution_> @NonNull SelectionSorter<Solution_, Object> determineSorter(
            @NonNull ValueSorterManner valueSorterManner, @NonNull GenuineVariableDescriptor<Solution_> variableDescriptor) {
        SelectionSorter<Solution_, Object> sorter = switch (valueSorterManner) {
            case NONE -> throw new IllegalStateException("Impossible state: hasSorter() should have returned null.");
            case INCREASING_STRENGTH, INCREASING_STRENGTH_IF_AVAILABLE, ASCENDING, ASCENDING_IF_AVAILABLE ->
                variableDescriptor.getAscendingSorter();
            case DECREASING_STRENGTH, DECREASING_STRENGTH_IF_AVAILABLE, DESCENDING, DESCENDING_IF_AVAILABLE ->
                variableDescriptor.getDescendingSorter();
        };
        if (sorter == null) {
            throw new IllegalArgumentException(
                    "The sorterManner (%s) on entity class (%s)'s variable (%s) fails because that variable getter's @%s annotation does not declare any strength comparison."
                            .formatted(valueSorterManner, variableDescriptor.getEntityDescriptor().getEntityClass(),
                                    variableDescriptor.getVariableName(), PlanningVariable.class.getSimpleName()));
        }
        return sorter;
    }

    @Override
    public boolean hasNearbySelectionConfig() {
        return nearbySelectionConfig != null;
    }
}
