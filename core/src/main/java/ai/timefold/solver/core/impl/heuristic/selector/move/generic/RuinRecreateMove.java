package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.move.director.VariableChangeRecordingScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

public final class RuinRecreateMove<Solution_> extends AbstractMove<Solution_> {

    private final GenuineVariableDescriptor<Solution_> genuineVariableDescriptor;
    private final RuinRecreateConstructionHeuristicPhaseBuilder<Solution_> constructionHeuristicPhaseBuilder;
    private final SolverScope<Solution_> solverScope;
    private final List<Object> ruinedEntityList;
    private final Set<Object> affectedValueSet;

    private Object[] recordedNewValues;

    public RuinRecreateMove(GenuineVariableDescriptor<Solution_> genuineVariableDescriptor,
            RuinRecreateConstructionHeuristicPhaseBuilder<Solution_> constructionHeuristicPhaseBuilder,
            SolverScope<Solution_> solverScope, List<Object> ruinedEntityList, Set<Object> affectedValueSet) {
        this.genuineVariableDescriptor = genuineVariableDescriptor;
        this.ruinedEntityList = ruinedEntityList;
        this.affectedValueSet = affectedValueSet;
        this.constructionHeuristicPhaseBuilder = constructionHeuristicPhaseBuilder;
        this.solverScope = solverScope;
        this.recordedNewValues = null;
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector) {
        recordedNewValues = new Object[ruinedEntityList.size()];

        var recordingScoreDirector = (VariableChangeRecordingScoreDirector<Solution_, ?>) scoreDirector;
        for (var ruinedEntity : ruinedEntityList) {
            recordingScoreDirector.beforeVariableChanged(genuineVariableDescriptor, ruinedEntity);
            genuineVariableDescriptor.setValue(ruinedEntity, null);
            recordingScoreDirector.afterVariableChanged(genuineVariableDescriptor, ruinedEntity);
        }
        recordingScoreDirector.triggerVariableListeners();

        var constructionHeuristicPhase =
                (RuinRecreateConstructionHeuristicPhase<Solution_>) constructionHeuristicPhaseBuilder
                        .ensureThreadSafe(recordingScoreDirector.getBacking())
                        .withElementsToRecreate(ruinedEntityList)
                        .build();

        var nestedSolverScope = new SolverScope<Solution_>(solverScope.getClock());
        nestedSolverScope.setSolver(solverScope.getSolver());
        nestedSolverScope.setScoreDirector(recordingScoreDirector.getBacking());
        constructionHeuristicPhase.solvingStarted(nestedSolverScope);
        constructionHeuristicPhase.solve(nestedSolverScope);
        constructionHeuristicPhase.solvingEnded(nestedSolverScope);
        scoreDirector.triggerVariableListeners();

        for (var i = 0; i < ruinedEntityList.size(); i++) {
            recordedNewValues[i] = genuineVariableDescriptor.getValue(ruinedEntityList.get(i));
        }
    }

    @Override
    public Collection<?> getPlanningEntities() {
        return ruinedEntityList;
    }

    @Override
    public Collection<?> getPlanningValues() {
        return affectedValueSet;
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        return true;
    }

    @Override
    public Move<Solution_> rebase(ScoreDirector<Solution_> destinationScoreDirector) {
        var rebasedRuinedEntityList = rebaseList(ruinedEntityList, destinationScoreDirector);
        var rebasedAffectedValueSet = rebaseSet(affectedValueSet, destinationScoreDirector);
        return new RuinRecreateMove<>(genuineVariableDescriptor, constructionHeuristicPhaseBuilder, solverScope,
                rebasedRuinedEntityList, rebasedAffectedValueSet);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof RuinRecreateMove<?> that))
            return false;
        return Objects.equals(genuineVariableDescriptor, that.genuineVariableDescriptor)
                && Objects.equals(ruinedEntityList, that.ruinedEntityList)
                && Objects.equals(affectedValueSet, that.affectedValueSet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(genuineVariableDescriptor, ruinedEntityList, affectedValueSet);
    }

    @Override
    public String toString() {
        return "RuinMove{" +
                "entities=" + ruinedEntityList +
                ", newValues=" + ((recordedNewValues != null) ? Arrays.toString(recordedNewValues) : "?") +
                '}';
    }
}
