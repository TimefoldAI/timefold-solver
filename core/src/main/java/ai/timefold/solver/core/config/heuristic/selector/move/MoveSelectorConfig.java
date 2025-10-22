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
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.SubChainChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.SubChainSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.TailChainSwapMoveSelectorConfig;
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
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorterWeightFactory;

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
        SubChainChangeMoveSelectorConfig.class,
        SubChainSwapMoveSelectorConfig.class,
        SubListChangeMoveSelectorConfig.class,
        SubListSwapMoveSelectorConfig.class,
        SwapMoveSelectorConfig.class,
        TailChainSwapMoveSelectorConfig.class,
        UnionMoveSelectorConfig.class
})
@XmlType(propOrder = {
        "cacheType",
        "selectionOrder",
        "filterClass",
        "sorterComparatorClass",
        "comparatorClass",
        "sorterWeightFactoryClass",
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
    protected Class<? extends SelectionFilter> filterClass = null;

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
    protected Class<? extends SelectionSorterWeightFactory> sorterWeightFactoryClass = null;
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
        return filterClass;
    }

    public void setFilterClass(@Nullable Class<? extends SelectionFilter> filterClass) {
        this.filterClass = filterClass;
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
    public @Nullable Class<? extends SelectionSorterWeightFactory> getSorterWeightFactoryClass() {
        return sorterWeightFactoryClass;
    }

    /**
     * @deprecated Deprecated in favor of {@link #setComparatorFactoryClass(Class)}
     * @param sorterWeightFactoryClass the class
     */
    @Deprecated(forRemoval = true, since = "1.28.0")
    public void setSorterWeightFactoryClass(@Nullable Class<? extends SelectionSorterWeightFactory> sorterWeightFactoryClass) {
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
        this.filterClass = filterClass;
        return (Config_) this;
    }

    /**
     * @deprecated Deprecated in favor of {@link #withComparatorClass(Class)}
     */
    @Deprecated(forRemoval = true, since = "1.28.0")
    public Config_ withSorterComparatorClass(Class<? extends Comparator> sorterComparatorClass) {
        this.sorterComparatorClass = sorterComparatorClass;
        return (Config_) this;
    }

    public Config_ withComparatorClass(Class<? extends Comparator> comparatorClass) {
        this.setComparatorClass(comparatorClass);
        return (Config_) this;
    }

    /**
     * @deprecated Deprecated in favor of {@link #withComparatorFactoryClass(Class)}
     * @param sorterWeightFactoryClass the factory class
     */
    @Deprecated(forRemoval = true, since = "1.28.0")
    public Config_ withSorterWeightFactoryClass(
            Class<? extends SelectionSorterWeightFactory> sorterWeightFactoryClass) {
        this.sorterWeightFactoryClass = sorterWeightFactoryClass;
        return (Config_) this;
    }

    public Config_
            withComparatorFactoryClass(Class<? extends ComparatorFactory> comparatorFactoryClass) {
        this.setComparatorFactoryClass(comparatorFactoryClass);
        return (Config_) this;
    }

    public Config_ withSorterOrder(SelectionSorterOrder sorterOrder) {
        this.sorterOrder = sorterOrder;
        return (Config_) this;
    }

    public Config_ withSorterClass(Class<? extends SelectionSorter> sorterClass) {
        this.sorterClass = sorterClass;
        return (Config_) this;
    }

    public Config_ withProbabilityWeightFactoryClass(
            Class<? extends SelectionProbabilityWeightFactory> probabilityWeightFactoryClass) {
        this.probabilityWeightFactoryClass = probabilityWeightFactoryClass;
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
        classVisitor.accept(filterClass);
        classVisitor.accept(sorterComparatorClass);
        classVisitor.accept(comparatorClass);
        classVisitor.accept(sorterWeightFactoryClass);
        classVisitor.accept(comparatorFactoryClass);
        classVisitor.accept(sorterClass);
        classVisitor.accept(probabilityWeightFactoryClass);
    }

    private void inheritCommon(MoveSelectorConfig<?> inheritedConfig) {
        cacheType = ConfigUtils.inheritOverwritableProperty(cacheType, inheritedConfig.getCacheType());
        selectionOrder = ConfigUtils.inheritOverwritableProperty(selectionOrder, inheritedConfig.getSelectionOrder());
        filterClass = ConfigUtils.inheritOverwritableProperty(filterClass, inheritedConfig.getFilterClass());
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

        fixedProbabilityWeight = ConfigUtils.inheritOverwritableProperty(
                fixedProbabilityWeight, inheritedConfig.getFixedProbabilityWeight());
    }

}
