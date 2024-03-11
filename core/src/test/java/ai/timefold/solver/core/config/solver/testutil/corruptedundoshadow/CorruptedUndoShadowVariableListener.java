package ai.timefold.solver.core.config.solver.testutil.corruptedundoshadow;

import java.util.Objects;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;

public class CorruptedUndoShadowVariableListener
        implements VariableListener<CorruptedUndoShadowSolution, CorruptedUndoShadowEntity> {
    @Override
    public void beforeEntityAdded(ScoreDirector<CorruptedUndoShadowSolution> scoreDirector,
            CorruptedUndoShadowEntity corruptedUndoShadowEntity) {

    }

    @Override
    public void afterEntityAdded(ScoreDirector<CorruptedUndoShadowSolution> scoreDirector,
            CorruptedUndoShadowEntity corruptedUndoShadowEntity) {
        update(scoreDirector, corruptedUndoShadowEntity);
    }

    @Override
    public void beforeEntityRemoved(ScoreDirector<CorruptedUndoShadowSolution> scoreDirector,
            CorruptedUndoShadowEntity corruptedUndoShadowEntity) {

    }

    @Override
    public void afterEntityRemoved(ScoreDirector<CorruptedUndoShadowSolution> scoreDirector,
            CorruptedUndoShadowEntity corruptedUndoShadowEntity) {

    }

    @Override
    public void beforeVariableChanged(ScoreDirector<CorruptedUndoShadowSolution> scoreDirector,
            CorruptedUndoShadowEntity corruptedUndoShadowEntity) {

    }

    @Override
    public void afterVariableChanged(ScoreDirector<CorruptedUndoShadowSolution> scoreDirector,
            CorruptedUndoShadowEntity corruptedUndoShadowEntity) {
        update(scoreDirector, corruptedUndoShadowEntity);
    }

    private void update(ScoreDirector<CorruptedUndoShadowSolution> scoreDirector,
            CorruptedUndoShadowEntity corruptedUndoShadowEntity) {
        if (corruptedUndoShadowEntity.valueClone == null || !Objects.equals("v1", corruptedUndoShadowEntity.value.value)) {
            scoreDirector.beforeVariableChanged(corruptedUndoShadowEntity, "valueClone");
            corruptedUndoShadowEntity.valueClone = corruptedUndoShadowEntity.value;
            scoreDirector.afterVariableChanged(corruptedUndoShadowEntity, "valueClone");
        }
    }
}
