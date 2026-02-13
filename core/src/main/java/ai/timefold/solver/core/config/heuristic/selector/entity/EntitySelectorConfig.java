package ai.timefold.solver.core.config.heuristic.selector.entity;

import java.util.Comparator;
import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.api.domain.common.ComparatorFactory;
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

import org.jspecify.annotations.NullMarked;
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
        "comparatorClass",
        "comparatorFactoryClass",
        "sorterOrder",
        "sorterClass",
        "probabilityWeightFactoryClass",
        "selectedCountLimit"
})
@NullMarked
public class EntitySelectorConfig extends SelectorConfig<EntitySelectorConfig> {

    public static EntitySelectorConfig newMimicSelectorConfig(String mimicSelectorRef) {
        return new EntitySelectorConfig()
                .withMimicSelectorRef(mimicSelectorRef);
    }

    @Nullable
    @XmlAttribute
    protected String id = null;
    @XmlAttribute
    @Nullable
    protected String mimicSelectorRef = null;

    @Nullable
    protected Class<?> entityClass = null;
    @Nullable
    protected SelectionCacheType cacheType = null;
    @Nullable
    protected SelectionOrder selectionOrder = null;

    @Nullable
    @XmlElement(name = "nearbySelection")
    protected NearbySelectionConfig nearbySelectionConfig = null;

    @Nullable
    protected Class<? extends SelectionFilter> filterClass = null;

    @Nullable
    protected EntitySorterManner sorterManner = null;
    @Nullable
    protected Class<? extends Comparator> comparatorClass = null;
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

    public @Nullable Class<? extends Comparator> getComparatorClass() {
        return comparatorClass;
    }

    public void setComparatorClass(@Nullable Class<? extends Comparator> comparatorClass) {
        this.comparatorClass = comparatorClass;
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

    public EntitySelectorConfig withId(String id) {
        this.setId(id);
        return this;
    }

    public EntitySelectorConfig withMimicSelectorRef(String mimicSelectorRef) {
        this.setMimicSelectorRef(mimicSelectorRef);
        return this;
    }

    public EntitySelectorConfig withEntityClass(Class<?> entityClass) {
        this.setEntityClass(entityClass);
        return this;
    }

    public EntitySelectorConfig withCacheType(SelectionCacheType cacheType) {
        this.setCacheType(cacheType);
        return this;
    }

    public EntitySelectorConfig withSelectionOrder(SelectionOrder selectionOrder) {
        this.setSelectionOrder(selectionOrder);
        return this;
    }

    public EntitySelectorConfig withNearbySelectionConfig(NearbySelectionConfig nearbySelectionConfig) {
        this.setNearbySelectionConfig(nearbySelectionConfig);
        return this;
    }

    public EntitySelectorConfig withFilterClass(Class<? extends SelectionFilter> filterClass) {
        this.setFilterClass(filterClass);
        return this;
    }

    public EntitySelectorConfig withSorterManner(EntitySorterManner sorterManner) {
        this.setSorterManner(sorterManner);
        return this;
    }

    public EntitySelectorConfig withComparatorClass(Class<? extends Comparator> comparatorClass) {
        this.setComparatorClass(comparatorClass);
        return this;
    }

    public EntitySelectorConfig
            withComparatorFactoryClass(Class<? extends ComparatorFactory> comparatorFactoryClass) {
        this.setComparatorFactoryClass(comparatorFactoryClass);
        return this;
    }

    public EntitySelectorConfig withSorterOrder(SelectionSorterOrder sorterOrder) {
        this.setSorterOrder(sorterOrder);
        return this;
    }

    public EntitySelectorConfig withSorterClass(Class<? extends SelectionSorter> sorterClass) {
        this.setSorterClass(sorterClass);
        return this;
    }

    public EntitySelectorConfig
            withProbabilityWeightFactoryClass(Class<? extends SelectionProbabilityWeightFactory> factoryClass) {
        this.setProbabilityWeightFactoryClass(factoryClass);
        return this;
    }

    public EntitySelectorConfig withSelectedCountLimit(long selectedCountLimit) {
        this.setSelectedCountLimit(selectedCountLimit);
        return this;
    }

    // ************************************************************************
    // Builder methods
    // ************************************************************************

    @Override
    public EntitySelectorConfig inherit(EntitySelectorConfig inheritedConfig) {
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
        comparatorClass = ConfigUtils.inheritOverwritableProperty(
                comparatorClass, inheritedConfig.getComparatorClass());
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
    public EntitySelectorConfig copyConfig() {
        return new EntitySelectorConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(Consumer<Class<?>> classVisitor) {
        classVisitor.accept(entityClass);
        if (nearbySelectionConfig != null) {
            nearbySelectionConfig.visitReferencedClasses(classVisitor);
        }
        classVisitor.accept(filterClass);
        classVisitor.accept(comparatorClass);
        classVisitor.accept(comparatorFactoryClass);
        classVisitor.accept(sorterClass);
        classVisitor.accept(probabilityWeightFactoryClass);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + entityClass + ")";
    }

    public static <Solution_> boolean hasSorter(EntitySorterManner entitySorterManner,
            EntityDescriptor<Solution_> entityDescriptor) {
        return switch (entitySorterManner) {
            case NONE -> false;
            case DESCENDING -> true;
            case DESCENDING_IF_AVAILABLE ->
                entityDescriptor.getDescendingSorter() != null;
        };
    }

    public static <Solution_, T> SelectionSorter<Solution_, T> determineSorter(EntitySorterManner entitySorterManner,
            EntityDescriptor<Solution_> entityDescriptor) {
        return switch (entitySorterManner) {
            case NONE -> throw new IllegalStateException("Impossible state: hasSorter() should have returned null.");
            case DESCENDING, DESCENDING_IF_AVAILABLE -> {
                var sorter = (SelectionSorter<Solution_, T>) entityDescriptor.getDescendingSorter();
                if (sorter == null) {
                    throw new IllegalArgumentException(
                            "The sorterManner (%s) on entity class (%s) fails because that entity class's @%s annotation does not declare any difficulty comparison."
                                    .formatted(entitySorterManner, entityDescriptor.getEntityClass(),
                                            PlanningEntity.class.getSimpleName()));
                }
                yield sorter;
            }
        };
    }

    @Override
    public boolean hasNearbySelectionConfig() {
        return nearbySelectionConfig != null;
    }
}
