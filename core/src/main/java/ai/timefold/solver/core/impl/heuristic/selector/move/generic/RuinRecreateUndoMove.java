package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import java.util.List;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractUndoMove;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;

public final class RuinRecreateUndoMove<Solution_> extends AbstractUndoMove<Solution_> {

    private final GenuineVariableDescriptor<Solution_> genuineVariableDescriptor;
    private final List<Object> ruinedEntityList;
    private final Object[] recordedNewValues;

    RuinRecreateUndoMove(RuinRecreateMove<Solution_> parentMove, GenuineVariableDescriptor<Solution_> genuineVariableDescriptor,
            List<Object> ruinedEntityList, Object[] recordedNewValues) {
        super(parentMove);
        this.genuineVariableDescriptor = genuineVariableDescriptor;
        this.ruinedEntityList = ruinedEntityList;
        this.recordedNewValues = recordedNewValues;
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector) {
        var innerScoreDirector = (VariableDescriptorAwareScoreDirector<Solution_>) scoreDirector;
        for (int i = 0; i < ruinedEntityList.size(); i++) {
            var entity = ruinedEntityList.get(i);
            innerScoreDirector.beforeVariableChanged(genuineVariableDescriptor, entity);
            genuineVariableDescriptor.setValue(entity, recordedNewValues[i]);
            innerScoreDirector.afterVariableChanged(genuineVariableDescriptor, entity);
        }
    }

}
