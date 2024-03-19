package ai.timefold.solver.core.impl.testdata.domain.clone.planning_cloneable;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.impl.domain.solution.cloner.PlanningCloneable;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

@PlanningSolution
public class PlanningCloneableSolution implements PlanningCloneable<PlanningCloneableSolution> {
    public static SolutionDescriptor<PlanningCloneableSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(PlanningCloneableSolution.class, PlanningCloneableEntity.class);
    }

    @PlanningEntityCollectionProperty
    public PlanningCloneableList<PlanningCloneableEntity> entityList;
    @ValueRangeProvider
    public PlanningCloneableList<TestdataValue> valueList;
    public PlanningCloneableMap<String, PlanningCloneableEntity> codeToEntity;

    @PlanningScore
    public HardSoftScore score;

    public PlanningCloneableSolution(List<PlanningCloneableEntity> entityList) {
        this.entityList = new PlanningCloneableList<>();
        this.valueList = new PlanningCloneableList<>();
        this.codeToEntity = new PlanningCloneableMap<>();
        this.score = null;
        for (PlanningCloneableEntity entity : entityList) {
            this.entityList.add(entity);
            this.codeToEntity.put(entity.getCode(), entity);
        }
    }

    @Override
    public PlanningCloneableSolution createNewInstance() {
        return new PlanningCloneableSolution(List.of());
    }
}
