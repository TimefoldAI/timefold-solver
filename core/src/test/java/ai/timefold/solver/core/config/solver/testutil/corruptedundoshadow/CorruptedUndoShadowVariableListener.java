package ai.timefold.solver.core.config.solver.testutil.corruptedundoshadow;

import java.util.Objects;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;

import org.jspecify.annotations.NonNull;

public class CorruptedUndoShadowVariableListener
        implements VariableListener<CorruptedUndoShadowSolution, CorruptedUndoShadowEntity> {
    @Override
    public void beforeEntityAdded(@NonNull ScoreDirector<CorruptedUndoShadowSolution> scoreDirector,
            @NonNull CorruptedUndoShadowEntity corruptedUndoShadowEntity) {

    }

    @Override
    public void afterEntityAdded(@NonNull ScoreDirector<CorruptedUndoShadowSolution> scoreDirector,
            @NonNull CorruptedUndoShadowEntity corruptedUndoShadowEntity) {
        update(scoreDirector, corruptedUndoShadowEntity);
    }

    @Override
    public void beforeEntityRemoved(@NonNull ScoreDirector<CorruptedUndoShadowSolution> scoreDirector,
            @NonNull CorruptedUndoShadowEntity corruptedUndoShadowEntity) {

    }

    @Override
    public void afterEntityRemoved(@NonNull ScoreDirector<CorruptedUndoShadowSolution> scoreDirector,
            @NonNull CorruptedUndoShadowEntity corruptedUndoShadowEntity) {

    }

    @Override
    public void beforeVariableChanged(@NonNull ScoreDirector<CorruptedUndoShadowSolution> scoreDirector,
            @NonNull CorruptedUndoShadowEntity corruptedUndoShadowEntity) {

    }

    @Override
    public void afterVariableChanged(@NonNull ScoreDirector<CorruptedUndoShadowSolution> scoreDirector,
            @NonNull CorruptedUndoShadowEntity corruptedUndoShadowEntity) {
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
