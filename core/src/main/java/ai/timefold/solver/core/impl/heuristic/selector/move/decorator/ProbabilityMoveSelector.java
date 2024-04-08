package ai.timefold.solver.core.impl.heuristic.selector.move.decorator;

import java.util.Iterator;
import java.util.NavigableMap;
import java.util.TreeMap;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.common.SelectionCacheLifecycleBridge;
import ai.timefold.solver.core.impl.heuristic.selector.common.SelectionCacheLifecycleListener;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionProbabilityWeightFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.AbstractMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.solver.random.RandomUtils;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

public class ProbabilityMoveSelector<Solution_> extends AbstractMoveSelector<Solution_>
        implements SelectionCacheLifecycleListener<Solution_> {

    protected final MoveSelector<Solution_> childMoveSelector;
    protected final SelectionCacheType cacheType;
    protected final SelectionProbabilityWeightFactory<Solution_, Move<Solution_>> probabilityWeightFactory;

    protected NavigableMap<Double, Move<Solution_>> cachedMoveMap = null;
    protected double probabilityWeightTotal = -1.0;

    public ProbabilityMoveSelector(MoveSelector<Solution_> childMoveSelector, SelectionCacheType cacheType,
            SelectionProbabilityWeightFactory<Solution_, ? extends Move<Solution_>> probabilityWeightFactory) {
        this.childMoveSelector = childMoveSelector;
        this.cacheType = cacheType;
        this.probabilityWeightFactory =
                (SelectionProbabilityWeightFactory<Solution_, Move<Solution_>>) probabilityWeightFactory;
        if (childMoveSelector.isNeverEnding()) {
            throw new IllegalStateException("The selector (" + this
                    + ") has a childMoveSelector (" + childMoveSelector
                    + ") with neverEnding (" + childMoveSelector.isNeverEnding() + ").");
        }
        phaseLifecycleSupport.addEventListener(childMoveSelector);
        if (cacheType.isNotCached()) {
            throw new IllegalArgumentException("The selector (" + this
                    + ") does not support the cacheType (" + cacheType + ").");
        }
        phaseLifecycleSupport.addEventListener(new SelectionCacheLifecycleBridge(cacheType, this));
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
        cachedMoveMap = new TreeMap<>();
        ScoreDirector<Solution_> scoreDirector = solverScope.getScoreDirector();
        double probabilityWeightOffset = 0L;
        for (Move<Solution_> entity : childMoveSelector) {
            double probabilityWeight = probabilityWeightFactory.createProbabilityWeight(scoreDirector, entity);
            cachedMoveMap.put(probabilityWeightOffset, entity);
            probabilityWeightOffset += probabilityWeight;
        }
        probabilityWeightTotal = probabilityWeightOffset;
    }

    @Override
    public void disposeCache(SolverScope<Solution_> solverScope) {
        probabilityWeightTotal = -1.0;
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
        return cachedMoveMap.size();
    }

    @Override
    public Iterator<Move<Solution_>> iterator() {
        return new Iterator<Move<Solution_>>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Move<Solution_> next() {
                double randomOffset = RandomUtils.nextDouble(workingRandom, probabilityWeightTotal);
                // entry is never null because randomOffset < probabilityWeightTotal
                return cachedMoveMap.floorEntry(randomOffset)
                        .getValue();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("The optional operation remove() is not supported.");
            }
        };
    }

    @Override
    public String toString() {
        return "Probability(" + childMoveSelector + ")";
    }

}
