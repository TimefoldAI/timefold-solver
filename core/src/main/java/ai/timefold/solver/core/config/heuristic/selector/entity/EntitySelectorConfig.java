package ai.timefold.solver.core.config.heuristic.selector.entity;

import java.util.Comparator;
import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.config.heuristic.selector.SelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.common.decorator.SelectionSorterOrder;
import ai.timefold.solver.core.config.heuristic.selector.common.nearby.NearbySelectionConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionProbabilityWeightFactory;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorter;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorterWeightFactory;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "id",
        "mimicSelectorRef",
        "entityClass",
        "cacheType",
        "selectionOrder",
        "nearbySelectionConfig",
        "filterClass",
        "sorterManner",
        "sorterComparatorClass",
        "sorterWeightFactoryClass",
        "sorterOrder",
        "sorterClass",
        "probabilityWeightFactoryClass",
        "selectedCountLimit"
})
public class EntitySelectorConfig extends SelectorConfig<EntitySelectorConfig> {

    public static EntitySelectorConfig newMimicSelectorConfig(String mimicSelectorRef) {
        return new EntitySelectorConfig()
                .withMimicSelectorRef(mimicSelectorRef);
    }

    @XmlAttribute
    protected String id = null;
    @XmlAttribute
    protected String mimicSelectorRef = null;

    protected Class<?> entityClass = null;

    protected SelectionCacheType cacheType = null;
    protected SelectionOrder selectionOrder = null;

    @XmlElement(name = "nearbySelection")
    protected NearbySelectionConfig nearbySelectionConfig = null;

    protected Class<? extends SelectionFilter> filterClass = null;

    protected EntitySorterManner sorterManner = null;
    protected Class<? extends Comparator> sorterComparatorClass = null;
    protected Class<? extends SelectionSorterWeightFactory> sorterWeightFactoryClass = null;
    protected SelectionSorterOrder sorterOrder = null;
    protected Class<? extends SelectionSorter> sorterClass = null;

    protected Class<? extends SelectionProbabilityWeightFactory> probabilityWeightFactoryClass = null;

    protected Long selectedCountLimit = null;

    public EntitySelectorConfig() {
    }

    public EntitySelectorConfig(Class<?> entityClass) {
        this.entityClass = entityClass;
    }

    public EntitySelectorConfig(@Nullable EntitySelectorConfig inheritedConfig) {
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

    public @Nullable Class<?> getEntityClass() {
        return entityClass;
    }

    public void setEntityClass(@Nullable Class<?> entityClass) {
        this.entityClass = entityClass;
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

    public @Nullable EntitySorterManner getSorterManner() {
        return sorterManner;
    }

    public void setSorterManner(@Nullable EntitySorterManner sorterManner) {
        this.sorterManner = sorterManner;
    }

    public @Nullable Class<? extends Comparator> getSorterComparatorClass() {
        return sorterComparatorClass;
    }

    public void setSorterComparatorClass(@Nullable Class<? extends Comparator> sorterComparatorClass) {
        this.sorterComparatorClass = sorterComparatorClass;
    }

    public @Nullable Class<? extends SelectionSorterWeightFactory> getSorterWeightFactoryClass() {
        return sorterWeightFactoryClass;
    }

    public void setSorterWeightFactoryClass(@Nullable Class<? extends SelectionSorterWeightFactory> sorterWeightFactoryClass) {
        this.sorterWeightFactoryClass = sorterWeightFactoryClass;
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

    public @NonNull EntitySelectorConfig withId(@NonNull String id) {
        this.setId(id);
        return this;
    }

    public @NonNull EntitySelectorConfig withMimicSelectorRef(@NonNull String mimicSelectorRef) {
        this.setMimicSelectorRef(mimicSelectorRef);
        return this;
    }

    public @NonNull EntitySelectorConfig withEntityClass(@NonNull Class<?> entityClass) {
        this.setEntityClass(entityClass);
        return this;
    }

    public @NonNull EntitySelectorConfig withCacheType(@NonNull SelectionCacheType cacheType) {
        this.setCacheType(cacheType);
        return this;
    }

    public @NonNull EntitySelectorConfig withSelectionOrder(@NonNull SelectionOrder selectionOrder) {
        this.setSelectionOrder(selectionOrder);
        return this;
    }

    public @NonNull EntitySelectorConfig withNearbySelectionConfig(@NonNull NearbySelectionConfig nearbySelectionConfig) {
        this.setNearbySelectionConfig(nearbySelectionConfig);
        return this;
    }

    public @NonNull EntitySelectorConfig withFilterClass(@NonNull Class<? extends SelectionFilter> filterClass) {
        this.setFilterClass(filterClass);
        return this;
    }

    public @NonNull EntitySelectorConfig withSorterManner(@NonNull EntitySorterManner sorterManner) {
        this.setSorterManner(sorterManner);
        return this;
    }

    public @NonNull EntitySelectorConfig withSorterComparatorClass(@NonNull Class<? extends Comparator> comparatorClass) {
        this.setSorterComparatorClass(comparatorClass);
        return this;
    }

    public @NonNull EntitySelectorConfig
            withSorterWeightFactoryClass(@NonNull Class<? extends SelectionSorterWeightFactory> weightFactoryClass) {
        this.setSorterWeightFactoryClass(weightFactoryClass);
        return this;
    }

    public @NonNull EntitySelectorConfig withSorterOrder(@NonNull SelectionSorterOrder sorterOrder) {
        this.setSorterOrder(sorterOrder);
        return this;
    }

    public @NonNull EntitySelectorConfig withSorterClass(@NonNull Class<? extends SelectionSorter> sorterClass) {
        this.setSorterClass(sorterClass);
        return this;
    }

    public @NonNull EntitySelectorConfig
            withProbabilityWeightFactoryClass(@NonNull Class<? extends SelectionProbabilityWeightFactory> factoryClass) {
        this.setProbabilityWeightFactoryClass(factoryClass);
        return this;
    }

    public @NonNull EntitySelectorConfig withSelectedCountLimit(long selectedCountLimit) {
        this.setSelectedCountLimit(selectedCountLimit);
        return this;
    }

    // ************************************************************************
    // Builder methods
    // ************************************************************************

    @Override
    public @NonNull EntitySelectorConfig inherit(@NonNull EntitySelectorConfig inheritedConfig) {
        id = ConfigUtils.inheritOverwritableProperty(id, inheritedConfig.getId());
        mimicSelectorRef = ConfigUtils.inheritOverwritableProperty(mimicSelectorRef,
                inheritedConfig.getMimicSelectorRef());
        entityClass = ConfigUtils.inheritOverwritableProperty(entityClass,
                inheritedConfig.getEntityClass());
        nearbySelectionConfig = ConfigUtils.inheritConfig(nearbySelectionConfig, inheritedConfig.getNearbySelectionConfig());
        cacheType = ConfigUtils.inheritOverwritableProperty(cacheType, inheritedConfig.getCacheType());
        selectionOrder = ConfigUtils.inheritOverwritableProperty(selectionOrder, inheritedConfig.getSelectionOrder());
        filterClass = ConfigUtils.inheritOverwritableProperty(
                filterClass, inheritedConfig.getFilterClass());
        sorterManner = ConfigUtils.inheritOverwritableProperty(
                sorterManner, inheritedConfig.getSorterManner());
        sorterComparatorClass = ConfigUtils.inheritOverwritableProperty(
                sorterComparatorClass, inheritedConfig.getSorterComparatorClass());
        sorterWeightFactoryClass = ConfigUtils.inheritOverwritableProperty(
                sorterWeightFactoryClass, inheritedConfig.getSorterWeightFactoryClass());
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
    public @NonNull EntitySelectorConfig copyConfig() {
        return new EntitySelectorConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        classVisitor.accept(entityClass);
        if (nearbySelectionConfig != null) {
            nearbySelectionConfig.visitReferencedClasses(classVisitor);
        }
        classVisitor.accept(filterClass);
        classVisitor.accept(sorterComparatorClass);
        classVisitor.accept(sorterWeightFactoryClass);
        classVisitor.accept(sorterClass);
        classVisitor.accept(probabilityWeightFactoryClass);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + entityClass + ")";
    }

    public static <Solution_> boolean hasSorter(@NonNull EntitySorterManner entitySorterManner,
            @NonNull EntityDescriptor<Solution_> entityDescriptor) {
        switch (entitySorterManner) {
            case NONE:
                return false;
            case DECREASING_DIFFICULTY:
                return true;
            case DECREASING_DIFFICULTY_IF_AVAILABLE:
                return entityDescriptor.getDecreasingDifficultySorter() != null;
            default:
                throw new IllegalStateException("The sorterManner ("
                        + entitySorterManner + ") is not implemented.");
        }
    }

    public static <Solution_, T> @NonNull SelectionSorter<Solution_, T> determineSorter(
            @NonNull EntitySorterManner entitySorterManner, @NonNull EntityDescriptor<Solution_> entityDescriptor) {
        SelectionSorter<Solution_, T> sorter;
        switch (entitySorterManner) {
            case NONE:
                throw new IllegalStateException("Impossible state: hasSorter() should have returned null.");
            case DECREASING_DIFFICULTY:
            case DECREASING_DIFFICULTY_IF_AVAILABLE:
                sorter = (SelectionSorter<Solution_, T>) entityDescriptor.getDecreasingDifficultySorter();
                if (sorter == null) {
                    throw new IllegalArgumentException("The sorterManner (" + entitySorterManner
                            + ") on entity class (" + entityDescriptor.getEntityClass()
                            + ") fails because that entity class's @" + PlanningEntity.class.getSimpleName()
                            + " annotation does not declare any difficulty comparison.");
                }
                return sorter;
            default:
                throw new IllegalStateException("The sorterManner ("
                        + entitySorterManner + ") is not implemented.");
        }
    }

    @Override
    public boolean hasNearbySelectionConfig() {
        return nearbySelectionConfig != null;
    }
}
