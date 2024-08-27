package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
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
    protected Move<Solution_> createUndoMove(ScoreDirector<Solution_> scoreDirector) {
        var recordedOldValues = new Object[ruinedEntityList.size()];
        for (var i = 0; i < ruinedEntityList.size(); i++) {
            recordedOldValues[i] = genuineVariableDescriptor.getValue(ruinedEntityList.get(i));
        }
        return new RuinRecreateUndoMove<>(this, genuineVariableDescriptor, ruinedEntityList, recordedOldValues);
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector) {
        recordedNewValues = new Object[ruinedEntityList.size()];

        var innerScoreDirector = (VariableDescriptorAwareScoreDirector<Solution_>) scoreDirector;
        for (var ruinedEntity : ruinedEntityList) {
            innerScoreDirector.beforeVariableChanged(genuineVariableDescriptor, ruinedEntity);
            genuineVariableDescriptor.setValue(ruinedEntity, null);
            innerScoreDirector.afterVariableChanged(genuineVariableDescriptor, ruinedEntity);
        }
        innerScoreDirector.triggerVariableListeners();

        var constructionHeuristicPhase = constructionHeuristicPhaseBuilder.withElementsToRecreate(ruinedEntityList)
                .build();
        constructionHeuristicPhase.setSolver(solverScope.getSolver());
        constructionHeuristicPhase.solvingStarted(solverScope);
        constructionHeuristicPhase.solve(solverScope);
        constructionHeuristicPhase.solvingEnded(solverScope);
        innerScoreDirector.triggerVariableListeners();

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
    public String toString() {
        return "RuinMove{" +
                "entities=" + ruinedEntityList +
                ", newValues=" + ((recordedNewValues != null) ? Arrays.toString(recordedNewValues) : "?") +
                '}';
    }
}
