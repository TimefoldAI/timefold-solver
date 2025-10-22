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

import org.jspecify.annotations.NullMarked;
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
@NullMarked
public class ValueSelectorConfig extends SelectorConfig<ValueSelectorConfig> {

    @XmlAttribute
    @Nullable
    protected String id = null;
    @XmlAttribute
    @Nullable
    protected String mimicSelectorRef = null;

    @Nullable
    protected Class<?> downcastEntityClass = null;
    @XmlAttribute
    @Nullable
    protected String variableName = null;

    @Nullable
    protected SelectionCacheType cacheType = null;
    @Nullable
    protected SelectionOrder selectionOrder = null;

    @XmlElement(name = "nearbySelection")
    @Nullable
    protected NearbySelectionConfig nearbySelectionConfig = null;

    @Nullable
    protected Class<? extends SelectionFilter> filterClass = null;

    @Nullable
    protected ValueSorterManner sorterManner = null;
    /**
     * @deprecated Deprecated in favor of {@link #comparatorClass}.
     */
    @Deprecated(forRemoval = true, since = "1.28.0")
    @Nullable
    protected Class<? extends Comparator> sorterComparatorClass = null;
    @Nullable
    protected Class<? extends Comparator> comparatorClass = null;
    /**
     * @deprecated Deprecated in favor of {@link #comparatorFactoryClass}.
     */
    @Deprecated(forRemoval = true, since = "1.28.0")
    @Nullable
    protected Class<? extends ComparatorFactory> sorterWeightFactoryClass = null;
    @Nullable
    protected Class<? extends ComparatorFactory> comparatorFactoryClass = null;
    @Nullable
    protected SelectionSorterOrder sorterOrder = null;
    @Nullable
    protected Class<? extends SelectionSorter> sorterClass = null;

    @Nullable
    protected Class<? extends SelectionProbabilityWeightFactory> probabilityWeightFactoryClass = null;

    @Nullable
    protected Long selectedCountLimit = null;

    public ValueSelectorConfig() {
    }

    public ValueSelectorConfig(String variableName) {
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

    public @Nullable Class<? extends Comparator> getComparatorClass() {
        return comparatorClass;
    }

    public void setComparatorClass(@Nullable Class<? extends Comparator> comparatorClass) {
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

    public @Nullable Class<? extends ComparatorFactory> getComparatorFactoryClass() {
        return comparatorFactoryClass;
    }

    public void setComparatorFactoryClass(@Nullable Class<? extends ComparatorFactory> comparatorFactoryClass) {
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

    public ValueSelectorConfig withId(String id) {
        this.setId(id);
        return this;
    }

    public ValueSelectorConfig withMimicSelectorRef(String mimicSelectorRef) {
        this.setMimicSelectorRef(mimicSelectorRef);
        return this;
    }

    public ValueSelectorConfig withDowncastEntityClass(Class<?> entityClass) {
        this.setDowncastEntityClass(entityClass);
        return this;
    }

    public ValueSelectorConfig withVariableName(String variableName) {
        this.setVariableName(variableName);
        return this;
    }

    public ValueSelectorConfig withCacheType(SelectionCacheType cacheType) {
        this.setCacheType(cacheType);
        return this;
    }

    public ValueSelectorConfig withSelectionOrder(SelectionOrder selectionOrder) {
        this.setSelectionOrder(selectionOrder);
        return this;
    }

    public ValueSelectorConfig withNearbySelectionConfig(NearbySelectionConfig nearbySelectionConfig) {
        this.setNearbySelectionConfig(nearbySelectionConfig);
        return this;
    }

    public ValueSelectorConfig withFilterClass(Class<? extends SelectionFilter> filterClass) {
        this.setFilterClass(filterClass);
        return this;
    }

    public ValueSelectorConfig withSorterManner(ValueSorterManner sorterManner) {
        this.setSorterManner(sorterManner);
        return this;
    }

    /**
     * @deprecated Deprecated in favor of {@link #withComparatorClass(Class)}
     */
    @Deprecated(forRemoval = true, since = "1.28.0")
    public ValueSelectorConfig withSorterComparatorClass(Class<? extends Comparator> comparatorClass) {
        this.setSorterComparatorClass(comparatorClass);
        return this;
    }

    public ValueSelectorConfig withComparatorClass(Class<? extends Comparator> comparatorClass) {
        this.setComparatorClass(comparatorClass);
        return this;
    }

    /**
     * @deprecated Deprecated in favor of {@link #withComparatorFactoryClass(Class)}
     * @param weightFactoryClass the factory class
     */
    @Deprecated(forRemoval = true, since = "1.28.0")
    public ValueSelectorConfig
            withSorterWeightFactoryClass(Class<? extends ComparatorFactory> weightFactoryClass) {
        this.setSorterWeightFactoryClass(weightFactoryClass);
        return this;
    }

    public ValueSelectorConfig
            withComparatorFactoryClass(Class<? extends ComparatorFactory> comparatorFactoryClass) {
        this.setComparatorFactoryClass(comparatorFactoryClass);
        return this;
    }

    public ValueSelectorConfig withSorterOrder(SelectionSorterOrder sorterOrder) {
        this.setSorterOrder(sorterOrder);
        return this;
    }

    public ValueSelectorConfig withSorterClass(Class<? extends SelectionSorter> sorterClass) {
        this.setSorterClass(sorterClass);
        return this;
    }

    public ValueSelectorConfig
            withProbabilityWeightFactoryClass(Class<? extends SelectionProbabilityWeightFactory> factoryClass) {
        this.setProbabilityWeightFactoryClass(factoryClass);
        return this;
    }

    public ValueSelectorConfig withSelectedCountLimit(long selectedCountLimit) {
        this.setSelectedCountLimit(selectedCountLimit);
        return this;
    }

    // ************************************************************************
    // Builder methods
    // ************************************************************************

    @Override
    public ValueSelectorConfig inherit(ValueSelectorConfig inheritedConfig) {
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
    public ValueSelectorConfig copyConfig() {
        return new ValueSelectorConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(Consumer<Class<?>> classVisitor) {
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

    public static <Solution_> boolean hasSorter(ValueSorterManner valueSorterManner,
            GenuineVariableDescriptor<Solution_> variableDescriptor) {
        return switch (valueSorterManner) {
            case NONE -> false;
            case INCREASING_STRENGTH, DECREASING_STRENGTH, ASCENDING, DESCENDING -> true;
            case INCREASING_STRENGTH_IF_AVAILABLE, ASCENDING_IF_AVAILABLE ->
                variableDescriptor.getAscendingSorter() != null;
            case DECREASING_STRENGTH_IF_AVAILABLE, DESCENDING_IF_AVAILABLE ->
                variableDescriptor.getDescendingSorter() != null;
        };
    }

    public static <Solution_> SelectionSorter<Solution_, Object> determineSorter(
            ValueSorterManner valueSorterManner, GenuineVariableDescriptor<Solution_> variableDescriptor) {
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
