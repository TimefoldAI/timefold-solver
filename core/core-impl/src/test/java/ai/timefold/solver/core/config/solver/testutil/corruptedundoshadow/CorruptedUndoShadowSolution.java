package ai.timefold.solver.core.config.solver.testutil.corruptedundoshadow;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;

@PlanningSolution
public class CorruptedUndoShadowSolution {
    @PlanningEntityCollectionProperty
    List<CorruptedUndoShadowEntity> entityList;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<CorruptedUndoShadowValue> valueList;

    @PlanningScore
    SimpleScore score;

    public CorruptedUndoShadowSolution() {
    }

    public CorruptedUndoShadowSolution(List<CorruptedUndoShadowEntity> entityList, List<CorruptedUndoShadowValue> valueList) {
        this.entityList = entityList;
        this.valueList = valueList;
    }
}
