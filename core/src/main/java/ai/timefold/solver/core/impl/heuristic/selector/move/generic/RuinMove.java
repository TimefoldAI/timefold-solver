package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import java.util.Arrays;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.constructionheuristic.DefaultConstructionHeuristicPhase;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

public class RuinMove<Solution_> extends AbstractMove<Solution_> {
    protected final Object[] ruinedEntities;
    protected final GenuineVariableDescriptor<Solution_> genuineVariableDescriptor;
    protected final DefaultConstructionHeuristicPhase<Solution_> constructionHeuristicPhase;
    protected final SolverScope<Solution_> solverScope;

    protected Object[] recordedNewValues;

    public RuinMove(Object[] ruinedEntities, GenuineVariableDescriptor<Solution_> genuineVariableDescriptor,
            DefaultConstructionHeuristicPhase<Solution_> constructionHeuristicPhase, SolverScope<Solution_> solverScope) {
        this(ruinedEntities, genuineVariableDescriptor, constructionHeuristicPhase, solverScope, null);
    }

    public RuinMove(Object[] ruinedEntities, GenuineVariableDescriptor<Solution_> genuineVariableDescriptor,
            DefaultConstructionHeuristicPhase<Solution_> constructionHeuristicPhase, SolverScope<Solution_> solverScope,
            Object[] recordedNewValues) {
        this.ruinedEntities = ruinedEntities;
        this.genuineVariableDescriptor = genuineVariableDescriptor;
        this.constructionHeuristicPhase = constructionHeuristicPhase;
        this.solverScope = solverScope;
        this.recordedNewValues = recordedNewValues;
    }

    @Override
    protected Move<Solution_> createUndoMove(ScoreDirector<Solution_> scoreDirector) {
        Object[] recordedOldValues = new Object[ruinedEntities.length];
        for (int i = 0; i < ruinedEntities.length; i++) {
            recordedOldValues[i] = genuineVariableDescriptor.getValue(ruinedEntities[i]);
        }
        return new RuinMove<>(ruinedEntities, genuineVariableDescriptor, constructionHeuristicPhase, solverScope,
                recordedOldValues);
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector) {
        VariableDescriptorAwareScoreDirector<Solution_> innerScoreDirector =
                (VariableDescriptorAwareScoreDirector<Solution_>) scoreDirector;
        if (recordedNewValues != null) {
            for (int i = 0; i < ruinedEntities.length; i++) {
                innerScoreDirector.beforeVariableChanged(genuineVariableDescriptor, ruinedEntities[i]);
                genuineVariableDescriptor.setValue(ruinedEntities[i], recordedNewValues[i]);
                innerScoreDirector.afterVariableChanged(genuineVariableDescriptor, ruinedEntities[i]);
            }
        } else {
            recordedNewValues = new Object[ruinedEntities.length];
            for (Object ruinedEntity : ruinedEntities) {
                innerScoreDirector.beforeVariableChanged(genuineVariableDescriptor, ruinedEntity);
                genuineVariableDescriptor.setValue(ruinedEntity, null);
                innerScoreDirector.afterVariableChanged(genuineVariableDescriptor, ruinedEntity);
            }
            constructionHeuristicPhase.solve(solverScope);
            for (int i = 0; i < ruinedEntities.length; i++) {
                recordedNewValues[i] = genuineVariableDescriptor.getValue(ruinedEntities[i]);
            }
        }
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
