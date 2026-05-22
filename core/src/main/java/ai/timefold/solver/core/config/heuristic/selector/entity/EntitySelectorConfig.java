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
public final class EntitySelectorConfig extends SelectorConfig<EntitySelectorConfig> {

    public static EntitySelectorConfig newMimicSelectorConfig(String mimicSelectorRef) {
        return new EntitySelectorConfig()
                .withMimicSelectorRef(mimicSelectorRef);
    }

    @Nullable
    @XmlAttribute
    private String id = null;
    @XmlAttribute
    @Nullable
    private String mimicSelectorRef = null;

    @Nullable
    private String entityClass = null;
    @Nullable
    private SelectionCacheType cacheType = null;
    @Nullable
    private SelectionOrder selectionOrder = null;

    @Nullable
    @XmlElement(name = "nearbySelection")
    private NearbySelectionConfig nearbySelectionConfig = null;

    @Nullable
    private String filterClass = null;

    @Nullable
    private EntitySorterManner sorterManner = null;
    @Nullable
    private String comparatorClass = null;
    @Nullable
    private String comparatorFactoryClass = null;
    @Nullable
    private SelectionSorterOrder sorterOrder = null;
    @Nullable
    private String sorterClass = null;

    @Nullable
    private String probabilityWeightFactoryClass = null;

    @Nullable
    private Long selectedCountLimit = null;

    public EntitySelectorConfig() {
    }

    public EntitySelectorConfig(@Nullable Class<?> entityClass) {
        this.entityClass = entityClass == null ? null : entityClass.getName();
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
        return ConfigUtils.resolveClass(entityClass, "entityClass", this);
    }

    public void setEntityClass(@Nullable Class<?> entityClass) {
        this.entityClass = entityClass == null ? null : entityClass.getName();
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
        return ConfigUtils.resolveClass(filterClass, "filterClass", this);
    }

    public void setFilterClass(@Nullable Class<? extends SelectionFilter> filterClass) {
        this.filterClass = filterClass == null ? null : filterClass.getName();
    }

    public @Nullable EntitySorterManner getSorterManner() {
        return sorterManner;
    }

    public void setSorterManner(@Nullable EntitySorterManner sorterManner) {
        this.sorterManner = sorterManner;
    }

    public @Nullable Class<? extends Comparator> getComparatorClass() {
        return ConfigUtils.resolveClass(comparatorClass, "comparatorClass", this);
    }

    public void setComparatorClass(@Nullable Class<? extends Comparator> comparatorClass) {
        this.comparatorClass = comparatorClass == null ? null : comparatorClass.getName();
    }

    public @Nullable Class<? extends ComparatorFactory> getComparatorFactoryClass() {
        return ConfigUtils.resolveClass(comparatorFactoryClass, "comparatorFactoryClass", this);
    }

    public void setComparatorFactoryClass(@Nullable Class<? extends ComparatorFactory> comparatorFactoryClass) {
        this.comparatorFactoryClass = comparatorFactoryClass == null ? null : comparatorFactoryClass.getName();
    }

    public @Nullable SelectionSorterOrder getSorterOrder() {
        return sorterOrder;
    }

    public void setSorterOrder(@Nullable SelectionSorterOrder sorterOrder) {
        this.sorterOrder = sorterOrder;
    }

    public @Nullable Class<? extends SelectionSorter> getSorterClass() {
        return ConfigUtils.resolveClass(sorterClass, "sorterClass", this);
    }

    public void setSorterClass(@Nullable Class<? extends SelectionSorter> sorterClass) {
        this.sorterClass = sorterClass == null ? null : sorterClass.getName();
    }

    public @Nullable Class<? extends SelectionProbabilityWeightFactory> getProbabilityWeightFactoryClass() {
        return ConfigUtils.resolveClass(probabilityWeightFactoryClass, "probabilityWeightFactoryClass", this);
    }

    public void setProbabilityWeightFactoryClass(
            @Nullable Class<? extends SelectionProbabilityWeightFactory> probabilityWeightFactoryClass) {
        this.probabilityWeightFactoryClass =
                probabilityWeightFactoryClass == null ? null : probabilityWeightFactoryClass.getName();
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
        this.entityClass = entityClass.getName();
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
        this.filterClass = filterClass.getName();
        return this;
    }

    public EntitySelectorConfig withSorterManner(EntitySorterManner sorterManner) {
        this.setSorterManner(sorterManner);
        return this;
    }

    public EntitySelectorConfig withComparatorClass(Class<? extends Comparator> comparatorClass) {
        this.comparatorClass = comparatorClass.getName();
        return this;
    }

    public EntitySelectorConfig withComparatorFactoryClass(Class<? extends ComparatorFactory> comparatorFactoryClass) {
        this.comparatorFactoryClass = comparatorFactoryClass.getName();
        return this;
    }

    public EntitySelectorConfig withSorterOrder(SelectionSorterOrder sorterOrder) {
        this.setSorterOrder(sorterOrder);
        return this;
    }

    public EntitySelectorConfig withSorterClass(Class<? extends SelectionSorter> sorterClass) {
        this.sorterClass = sorterClass.getName();
        return this;
    }

    public EntitySelectorConfig
            withProbabilityWeightFactoryClass(Class<? extends SelectionProbabilityWeightFactory> factoryClass) {
        this.probabilityWeightFactoryClass = factoryClass.getName();
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
                inheritedConfig.entityClass);
        nearbySelectionConfig = ConfigUtils.inheritConfig(nearbySelectionConfig, inheritedConfig.getNearbySelectionConfig());
        cacheType = ConfigUtils.inheritOverwritableProperty(cacheType, inheritedConfig.getCacheType());
        selectionOrder = ConfigUtils.inheritOverwritableProperty(selectionOrder, inheritedConfig.getSelectionOrder());
        filterClass = ConfigUtils.inheritOverwritableProperty(
                filterClass, inheritedConfig.filterClass);
        sorterManner = ConfigUtils.inheritOverwritableProperty(
                sorterManner, inheritedConfig.getSorterManner());
        comparatorClass = ConfigUtils.inheritOverwritableProperty(
                comparatorClass, inheritedConfig.comparatorClass);
        comparatorFactoryClass = ConfigUtils.inheritOverwritableProperty(
                comparatorFactoryClass, inheritedConfig.comparatorFactoryClass);
        sorterOrder = ConfigUtils.inheritOverwritableProperty(
                sorterOrder, inheritedConfig.getSorterOrder());
        sorterClass = ConfigUtils.inheritOverwritableProperty(
                sorterClass, inheritedConfig.sorterClass);
        probabilityWeightFactoryClass = ConfigUtils.inheritOverwritableProperty(
                probabilityWeightFactoryClass, inheritedConfig.probabilityWeightFactoryClass);
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
        classVisitor.accept(getEntityClass());
        if (nearbySelectionConfig != null) {
            nearbySelectionConfig.visitReferencedClasses(classVisitor);
        }
        classVisitor.accept(getFilterClass());
        classVisitor.accept(getComparatorClass());
        classVisitor.accept(getComparatorFactoryClass());
        classVisitor.accept(getSorterClass());
        classVisitor.accept(getProbabilityWeightFactoryClass());
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
