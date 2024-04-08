package ai.timefold.solver.core.impl.heuristic.selector.entity.decorator;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.AbstractDemandEnabledSelector;
import ai.timefold.solver.core.impl.heuristic.selector.common.SelectionCacheLifecycleBridge;
import ai.timefold.solver.core.impl.heuristic.selector.common.SelectionCacheLifecycleListener;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionProbabilityWeightFactory;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.solver.random.RandomUtils;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

public final class ProbabilityEntitySelector<Solution_>
        extends AbstractDemandEnabledSelector<Solution_>
        implements SelectionCacheLifecycleListener<Solution_>, EntitySelector<Solution_> {

    private final EntitySelector<Solution_> childEntitySelector;
    private final SelectionCacheType cacheType;
    private final SelectionProbabilityWeightFactory<Solution_, Object> probabilityWeightFactory;

    private NavigableMap<Double, Object> cachedEntityMap = null;
    private double probabilityWeightTotal = -1.0;

    public ProbabilityEntitySelector(EntitySelector<Solution_> childEntitySelector, SelectionCacheType cacheType,
            SelectionProbabilityWeightFactory<Solution_, Object> probabilityWeightFactory) {
        this.childEntitySelector = childEntitySelector;
        this.cacheType = cacheType;
        this.probabilityWeightFactory = probabilityWeightFactory;
        if (childEntitySelector.isNeverEnding()) {
            throw new IllegalStateException("The selector (" + this
                    + ") has a childEntitySelector (" + childEntitySelector
                    + ") with neverEnding (" + childEntitySelector.isNeverEnding() + ").");
        }
        phaseLifecycleSupport.addEventListener(childEntitySelector);
        if (cacheType.isNotCached()) {
            throw new IllegalArgumentException("The selector (" + this
                    + ") does not support the cacheType (" + cacheType + ").");
        }
        phaseLifecycleSupport.addEventListener(new SelectionCacheLifecycleBridge<>(cacheType, this));
    }

    @Override
    public SelectionCacheType getCacheType() {
        return cacheType;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public void constructCache(SolverScope<Solution_> solverScope) {
        cachedEntityMap = new TreeMap<>();
        ScoreDirector<Solution_> scoreDirector = solverScope.getScoreDirector();
        double probabilityWeightOffset = 0L;
        for (Object entity : childEntitySelector) {
            double probabilityWeight = probabilityWeightFactory.createProbabilityWeight(
                    scoreDirector, entity);
            cachedEntityMap.put(probabilityWeightOffset, entity);
            probabilityWeightOffset += probabilityWeight;
        }
        probabilityWeightTotal = probabilityWeightOffset;
    }

    @Override
    public void disposeCache(SolverScope<Solution_> solverScope) {
        probabilityWeightTotal = -1.0;
    }

    @Override
    public EntityDescriptor<Solution_> getEntityDescriptor() {
        return childEntitySelector.getEntityDescriptor();
    }

    @Override
    public boolean isCountable() {
        return true;
    }

    @Override
    public boolean isNeverEnding() {
        return true;
    }

    @Override
    public long getSize() {
        return cachedEntityMap.size();
    }

    @Override
    public Iterator<Object> iterator() {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Object next() {
                double randomOffset = RandomUtils.nextDouble(workingRandom, probabilityWeightTotal);
                // entry is never null because randomOffset < probabilityWeightTotal
                return cachedEntityMap.floorEntry(randomOffset)
                        .getValue();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("The optional operation remove() is not supported.");
            }
        };
    }

    @Override
    public ListIterator<Object> listIterator() {
        throw new IllegalStateException("The selector (" + this
                + ") does not support a ListIterator with randomSelection (true).");
    }

    @Override
    public ListIterator<Object> listIterator(int index) {
        throw new IllegalStateException("The selector (" + this
                + ") does not support a ListIterator with randomSelection (true).");
    }

    @Override
    public Iterator<Object> endingIterator() {
        return childEntitySelector.endingIterator();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (other == null || getClass() != other.getClass())
            return false;
        ProbabilityEntitySelector<?> that = (ProbabilityEntitySelector<?>) other;
        return Objects.equals(childEntitySelector, that.childEntitySelector) && cacheType == that.cacheType
                && Objects.equals(probabilityWeightFactory, that.probabilityWeightFactory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(childEntitySelector, cacheType, probabilityWeightFactory);
    }

    @Override
    public String toString() {
        return "Probability(" + childEntitySelector + ")";
    }

}
