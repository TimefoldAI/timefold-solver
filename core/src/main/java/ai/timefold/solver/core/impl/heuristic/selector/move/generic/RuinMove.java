package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import java.util.Arrays;
import java.util.Collection;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.constructionheuristic.RuinRecreateConstructionHeuristicPhase.RuinRecreateBuilderConstructionHeuristicPhaseBuilder;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

public final class RuinMove<Solution_> extends AbstractMove<Solution_> {

    private final Object[] ruinedEntities;
    private final GenuineVariableDescriptor<Solution_> genuineVariableDescriptor;
    private final RuinRecreateBuilderConstructionHeuristicPhaseBuilder<Solution_> constructionHeuristicPhaseBuilder;
    private final SolverScope<Solution_> solverScope;

    private Object[] recordedNewValues;

    public RuinMove(Object[] ruinedEntities, GenuineVariableDescriptor<Solution_> genuineVariableDescriptor,
            RuinRecreateBuilderConstructionHeuristicPhaseBuilder<Solution_> constructionHeuristicPhaseBuilder,
            SolverScope<Solution_> solverScope) {
        this.ruinedEntities = ruinedEntities;
        this.genuineVariableDescriptor = genuineVariableDescriptor;
        this.constructionHeuristicPhaseBuilder = constructionHeuristicPhaseBuilder;
        this.solverScope = solverScope;
        this.recordedNewValues = null;
    }

    @Override
    protected Move<Solution_> createUndoMove(ScoreDirector<Solution_> scoreDirector) {
        var recordedOldValues = new Object[ruinedEntities.length];
        for (var i = 0; i < ruinedEntities.length; i++) {
            recordedOldValues[i] = genuineVariableDescriptor.getValue(ruinedEntities[i]);
        }
        return new RuinUndoMove<>(ruinedEntities, genuineVariableDescriptor,
                recordedOldValues);
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector) {
        var innerScoreDirector = (VariableDescriptorAwareScoreDirector<Solution_>) scoreDirector;
        recordedNewValues = new Object[ruinedEntities.length];
        for (var ruinedEntity : ruinedEntities) {
            innerScoreDirector.beforeVariableChanged(genuineVariableDescriptor, ruinedEntity);
            genuineVariableDescriptor.setValue(ruinedEntity, null);
            innerScoreDirector.afterVariableChanged(genuineVariableDescriptor, ruinedEntity);
        }
        innerScoreDirector.triggerVariableListeners();

        var constructionHeuristicPhase = constructionHeuristicPhaseBuilder.setElementsToRecreate(ruinedEntities)
                .build();
        constructionHeuristicPhase.setSolver(solverScope.getSolver());
        constructionHeuristicPhase.solvingStarted(solverScope);
        constructionHeuristicPhase.solve(solverScope);
        constructionHeuristicPhase.solvingEnded(solverScope);
        innerScoreDirector.triggerVariableListeners();

        for (var i = 0; i < ruinedEntities.length; i++) {
            recordedNewValues[i] = genuineVariableDescriptor.getValue(ruinedEntities[i]);
        }
    }

    @Override
    public Collection<?> getPlanningEntities() {
        return Arrays.asList(ruinedEntities);
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        return true;
    }

    @Override
    public String toString() {
        return "RuinMove{" +
                "entities=" + Arrays.toString(ruinedEntities) +
                ", newValues=" + ((recordedNewValues != null) ? Arrays.toString(recordedNewValues) : "?") +
                '}';
    }
}
