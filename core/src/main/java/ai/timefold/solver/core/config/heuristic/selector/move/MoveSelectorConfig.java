package ai.timefold.solver.core.config.heuristic.selector.move;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.api.domain.common.ComparatorFactory;
import ai.timefold.solver.core.config.heuristic.selector.SelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.common.decorator.SelectionSorterOrder;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.CartesianProductMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.UnionMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.factory.MoveIteratorFactoryConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.factory.MoveListFactoryConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.ChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.PillarChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.PillarSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.RuinRecreateMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.SwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListRuinRecreateMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.SubListChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.SubListSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.kopt.KOptListMoveSelectorConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionProbabilityWeightFactory;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorter;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * General superclass for {@link ChangeMoveSelectorConfig}, etc.
 */

@XmlSeeAlso({
        CartesianProductMoveSelectorConfig.class,
        ChangeMoveSelectorConfig.class,
        KOptListMoveSelectorConfig.class,
        ListChangeMoveSelectorConfig.class,
        ListSwapMoveSelectorConfig.class,
        MoveIteratorFactoryConfig.class,
        MoveListFactoryConfig.class,
        PillarChangeMoveSelectorConfig.class,
        PillarSwapMoveSelectorConfig.class,
        RuinRecreateMoveSelectorConfig.class,
        ListRuinRecreateMoveSelectorConfig.class,
        SubListChangeMoveSelectorConfig.class,
        SubListSwapMoveSelectorConfig.class,
        SwapMoveSelectorConfig.class,
        UnionMoveSelectorConfig.class
})
@XmlType(propOrder = {
        "cacheType",
        "selectionOrder",
        "filterClass",
        "comparatorClass",
        "comparatorFactoryClass",
        "sorterOrder",
        "sorterClass",
        "probabilityWeightFactoryClass",
        "selectedCountLimit",
        "fixedProbabilityWeight"
})
@NullMarked
public abstract class MoveSelectorConfig<Config_ extends MoveSelectorConfig<Config_>> extends SelectorConfig<Config_> {

    @Nullable
    protected SelectionCacheType cacheType = null;
    @Nullable
    protected SelectionOrder selectionOrder = null;

    @Nullable
    protected String filterClass = null;

    @Nullable
    protected String comparatorClass = null;
    @Nullable
    protected String comparatorFactoryClass = null;
    @Nullable
    protected SelectionSorterOrder sorterOrder = null;
    @Nullable
    protected String sorterClass = null;

    @Nullable
    protected String probabilityWeightFactoryClass = null;

    @Nullable
    protected Long selectedCountLimit = null;

    @Nullable
    private Double fixedProbabilityWeight = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

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

    public @Nullable Class<? extends SelectionFilter> getFilterClass() {
        return ConfigUtils.resolveClass(filterClass, "filterClass", this);
    }

    public void setFilterClass(@Nullable Class<? extends SelectionFilter> filterClass) {
        this.filterClass = filterClass == null ? null : filterClass.getName();
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
        this.probabilityWeightFactoryClass = probabilityWeightFactoryClass == null ? null
                : probabilityWeightFactoryClass.getName();
    }

    public @Nullable Long getSelectedCountLimit() {
        return selectedCountLimit;
    }

    public void setSelectedCountLimit(@Nullable Long selectedCountLimit) {
        this.selectedCountLimit = selectedCountLimit;
    }

    public @Nullable Double getFixedProbabilityWeight() {
        return fixedProbabilityWeight;
    }

    public void setFixedProbabilityWeight(@Nullable Double fixedProbabilityWeight) {
        this.fixedProbabilityWeight = fixedProbabilityWeight;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public Config_ withCacheType(SelectionCacheType cacheType) {
        this.cacheType = cacheType;
        return (Config_) this;
    }

    public Config_ withSelectionOrder(SelectionOrder selectionOrder) {
        this.selectionOrder = selectionOrder;
        return (Config_) this;
    }

    public Config_ withFilterClass(Class<? extends SelectionFilter> filterClass) {
        this.filterClass = filterClass.getName();
        return (Config_) this;
    }

    public Config_ withComparatorClass(Class<? extends Comparator> comparatorClass) {
        this.comparatorClass = comparatorClass.getName();
        return (Config_) this;
    }

    public Config_
            withComparatorFactoryClass(Class<? extends ComparatorFactory> comparatorFactoryClass) {
        this.comparatorFactoryClass = comparatorFactoryClass.getName();
        return (Config_) this;
    }

    public Config_ withSorterOrder(SelectionSorterOrder sorterOrder) {
        this.sorterOrder = sorterOrder;
        return (Config_) this;
    }

    public Config_ withSorterClass(Class<? extends SelectionSorter> sorterClass) {
        this.sorterClass = sorterClass.getName();
        return (Config_) this;
    }

    public Config_ withProbabilityWeightFactoryClass(
            Class<? extends SelectionProbabilityWeightFactory> probabilityWeightFactoryClass) {
        this.probabilityWeightFactoryClass = probabilityWeightFactoryClass.getName();
        return (Config_) this;
    }

    public Config_ withSelectedCountLimit(Long selectedCountLimit) {
        this.selectedCountLimit = selectedCountLimit;
        return (Config_) this;
    }

    public Config_ withFixedProbabilityWeight(Double fixedProbabilityWeight) {
        this.fixedProbabilityWeight = fixedProbabilityWeight;
        return (Config_) this;
    }

    /**
     * Gather a list of all descendant {@link MoveSelectorConfig}s
     * except for {@link UnionMoveSelectorConfig} and {@link CartesianProductMoveSelectorConfig}.
     *
     */
    public void extractLeafMoveSelectorConfigsIntoList(List<MoveSelectorConfig> leafMoveSelectorConfigList) {
        leafMoveSelectorConfigList.add(this);
    }

    @Override
    public Config_ inherit(Config_ inheritedConfig) {
        inheritCommon(inheritedConfig);
        return (Config_) this;
    }

    /**
     * Does not inherit subclass properties because this class and {@code foldedConfig} can be of a different type.
     */
    public void inheritFolded(MoveSelectorConfig<?> foldedConfig) {
        inheritCommon(foldedConfig);
    }

    protected void visitCommonReferencedClasses(Consumer<Class<?>> classVisitor) {
        classVisitor.accept(getFilterClass());
        classVisitor.accept(getComparatorClass());
        classVisitor.accept(getComparatorFactoryClass());
        classVisitor.accept(getSorterClass());
        classVisitor.accept(getProbabilityWeightFactoryClass());
    }

    private void inheritCommon(MoveSelectorConfig<?> inheritedConfig) {
        cacheType = ConfigUtils.inheritOverwritableProperty(cacheType, inheritedConfig.getCacheType());
        selectionOrder = ConfigUtils.inheritOverwritableProperty(selectionOrder, inheritedConfig.getSelectionOrder());
        filterClass = ConfigUtils.inheritOverwritableProperty(filterClass, inheritedConfig.filterClass);
        comparatorClass = ConfigUtils.inheritOverwritableProperty(comparatorClass, inheritedConfig.comparatorClass);
        comparatorFactoryClass = ConfigUtils.inheritOverwritableProperty(
                comparatorFactoryClass, inheritedConfig.comparatorFactoryClass);
        sorterOrder = ConfigUtils.inheritOverwritableProperty(sorterOrder, inheritedConfig.getSorterOrder());
        sorterClass = ConfigUtils.inheritOverwritableProperty(sorterClass, inheritedConfig.sorterClass);
        probabilityWeightFactoryClass = ConfigUtils.inheritOverwritableProperty(
                probabilityWeightFactoryClass, inheritedConfig.probabilityWeightFactoryClass);
        selectedCountLimit = ConfigUtils.inheritOverwritableProperty(
                selectedCountLimit, inheritedConfig.getSelectedCountLimit());

        fixedProbabilityWeight = ConfigUtils.inheritOverwritableProperty(
                fixedProbabilityWeight, inheritedConfig.getFixedProbabilityWeight());
    }

}
