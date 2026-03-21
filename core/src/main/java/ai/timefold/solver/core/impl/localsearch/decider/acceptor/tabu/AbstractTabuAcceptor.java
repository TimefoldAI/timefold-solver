package ai.timefold.solver.core.impl.localsearch.decider.acceptor.tabu;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import ai.timefold.solver.core.impl.localsearch.decider.acceptor.AbstractAcceptor;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.Acceptor;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.tabu.size.TabuSizeStrategy;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.util.CollectionUtils;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Abstract superclass for all Tabu Acceptors.
 *
 * @see Acceptor
 */
@NullMarked
public abstract sealed class AbstractTabuAcceptor<Solution_>
        extends AbstractAcceptor<Solution_>
        permits EntityTabuAcceptor, MoveTabuAcceptor, ValueTabuAcceptor {

    private final String logIndentation;

    private @Nullable TabuSizeStrategy<Solution_> tabuSizeStrategy = null;
    private @Nullable TabuSizeStrategy<Solution_> fadingTabuSizeStrategy = null;
    private boolean aspirationEnabled = true;

    private boolean assertTabuHashCodeCorrectness = false;

    private Map<@Nullable Object, Integer> tabuToStepIndexMap = Collections.emptyMap(); // Avoid @Nullable.

    private int workingTabuSize = -1;
    private int workingFadingTabuSize = -1;

    protected AbstractTabuAcceptor(String logIndentation) {
        this.logIndentation = logIndentation;
    }

    public void setTabuSizeStrategy(TabuSizeStrategy<Solution_> tabuSizeStrategy) {
        this.tabuSizeStrategy = tabuSizeStrategy;
    }

    public void setFadingTabuSizeStrategy(TabuSizeStrategy<Solution_> fadingTabuSizeStrategy) {
        this.fadingTabuSizeStrategy = fadingTabuSizeStrategy;
    }

    public void setAspirationEnabled(boolean aspirationEnabled) {
        this.aspirationEnabled = aspirationEnabled;
    }

    public void setAssertTabuHashCodeCorrectness(boolean assertTabuHashCodeCorrectness) {
        this.assertTabuHashCodeCorrectness = assertTabuHashCodeCorrectness;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        var lastCompletedStepScope = phaseScope.getLastCompletedStepScope();
        // Tabu sizes do not change during stepStarted(), because they must be in sync with the tabuSequenceList.size()
        workingTabuSize = tabuSizeStrategy == null ? 0 : tabuSizeStrategy.determineTabuSize(lastCompletedStepScope);
        workingFadingTabuSize = fadingTabuSizeStrategy == null ? 0
                : fadingTabuSizeStrategy.determineTabuSize(lastCompletedStepScope);
        var totalTabuListSize = workingTabuSize + workingFadingTabuSize; // is at least 1
        tabuToStepIndexMap = CollectionUtils.newLinkedHashMap(totalTabuListSize);
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        tabuToStepIndexMap = Collections.emptyMap();
        workingTabuSize = -1;
        workingFadingTabuSize = -1;
    }

    @Override
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        // Tabu sizes do not change during stepStarted(), because they must be in sync with the tabuSequenceList.size()
        workingTabuSize = tabuSizeStrategy == null ? 0 : tabuSizeStrategy.determineTabuSize(stepScope);
        workingFadingTabuSize = fadingTabuSizeStrategy == null ? 0 : fadingTabuSizeStrategy.determineTabuSize(stepScope);
        adjustTabuList(stepScope.getStepIndex(), findNewTabu(stepScope));
    }

    protected void adjustTabuList(int tabuStepIndex, Collection<? extends Object> tabus) {
        var totalTabuListSize = workingTabuSize + workingFadingTabuSize; // is at least 1
        // Remove the oldest tabu(s).
        var it = tabuToStepIndexMap.keySet().iterator();
        while (it.hasNext()) {
            var oldTabu = it.next();
            var oldTabuStepIndexInteger = tabuToStepIndexMap.get(oldTabu);
            if (oldTabuStepIndexInteger == null) {
                // oldTabu not null here, as null is a valid key and therefore has a valid corresponding value.
                throw createHashcodeStabilityViolationException(oldTabu);
            }
            var oldTabuStepCount = tabuStepIndex - oldTabuStepIndexInteger; // at least 1
            if (oldTabuStepCount < totalTabuListSize) {
                break;
            }
            it.remove();
        }
        // Add the new tabu(s)
        for (var tabu : tabus) {
            // Push tabu to the end of the line; remove+put has that effect in LinkedHashMap.
            tabuToStepIndexMap.remove(tabu);
            tabuToStepIndexMap.put(tabu, tabuStepIndex);
        }
    }

    private static IllegalStateException createHashcodeStabilityViolationException(Object tabu) {
        return new IllegalStateException(
                "HashCode stability violation: the hashCode() of tabu (%s) of class (%s) changed during planning, since it was inserted in the tabu Map."
                        .formatted(tabu, tabu.getClass()));
    }

    @Override
    public boolean isAccepted(LocalSearchMoveScope<Solution_> moveScope) {
        var maximumTabuStepIndex = locateMaximumTabuStepIndex(moveScope);
        if (maximumTabuStepIndex < 0) {
            // The move isn't tabu at all
            return true;
        }
        // Natural comparison because shifting penalties don't apply
        if (aspirationEnabled &&
                moveScope.getScore().compareTo(moveScope.getStepScope().getPhaseScope().getBestScore()) > 0) {
            logger.trace("{}        Proposed move ({}) is tabu, but is accepted anyway due to aspiration.",
                    logIndentation, moveScope.getMove());
            return true;
        }
        var tabuStepCount = moveScope.getStepScope().getStepIndex() - maximumTabuStepIndex; // at least 1
        if (tabuStepCount <= workingTabuSize) {
            logger.trace("{}        Proposed move ({}) is tabu and is therefore not accepted.",
                    logIndentation, moveScope.getMove());
            return false;
        }
        var acceptChance = calculateFadingTabuAcceptChance(tabuStepCount - workingTabuSize);
        var accepted = moveScope.getWorkingRandom().nextDouble() < acceptChance;
        if (accepted) {
            logger.trace("{}        Proposed move ({}) is fading tabu with acceptChance ({}) and is accepted.",
                    logIndentation,
                    moveScope.getMove(), acceptChance);
        } else {
            logger.trace("{}        Proposed move ({}) is fading tabu with acceptChance ({}) and is not accepted.",
                    logIndentation,
                    moveScope.getMove(), acceptChance);
        }
        return accepted;
    }

    private int locateMaximumTabuStepIndex(LocalSearchMoveScope<Solution_> moveScope) {
        var checkingTabus = findTabu(moveScope);
        var maximumTabuStepIndex = -1;
        for (var checkingTabu : checkingTabus) {
            var tabuStepIndexInteger = tabuToStepIndexMap.get(checkingTabu);
            if (tabuStepIndexInteger != null) {
                maximumTabuStepIndex = Math.max(tabuStepIndexInteger, maximumTabuStepIndex);
            }
            if (assertTabuHashCodeCorrectness) {
                for (var tabu : tabuToStepIndexMap.keySet()) {
                    // tabu and checkingTabu can be null with a planning variable which allows unassigned values
                    if (tabu != null && tabu.equals(checkingTabu)) {
                        if (tabu.hashCode() != checkingTabu.hashCode()) {
                            throw new IllegalStateException(
                                    "HashCode/equals contract violation: tabu (%s) of class (%s) and checkingTabu (%s) are equals() but have a different hashCode()."
                                            .formatted(tabu, tabu.getClass(), checkingTabu));
                        }
                        if (tabuStepIndexInteger == null) {
                            throw createHashcodeStabilityViolationException(tabu);
                        }
                    }
                }
            }
        }
        return maximumTabuStepIndex;
    }

    /**
     * @param fadingTabuStepCount {@code 0 < fadingTabuStepCount <= fadingTabuSize}
     * @return {@code 0.0 < acceptChance < 1.0}
     */
    protected double calculateFadingTabuAcceptChance(int fadingTabuStepCount) {
        // The + 1's are because acceptChance should not be 0.0 or 1.0
        // when (fadingTabuStepCount == 0) or (fadingTabuStepCount + 1 == workingFadingTabuSize)
        return (workingFadingTabuSize - fadingTabuStepCount) / ((double) (workingFadingTabuSize + 1));
    }

    /**
     * @param moveScope
     * @return some tabu lists allow null, some don't; children will override
     */
    protected abstract Collection<? extends Object> findTabu(LocalSearchMoveScope<Solution_> moveScope);

    /**
     * @param stepScope
     * @return some tabu lists allow null, some don't; children will override
     */
    protected abstract Collection<? extends Object> findNewTabu(LocalSearchStepScope<Solution_> stepScope);

}
