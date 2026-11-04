package ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.entityrange;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.SimpleScore;

@PlanningSolution
public class TestdataEntityRangeSolution {

    @PlanningEntityCollectionProperty
    private List<TestdataEntityRangeBaseEntity> entityList;
    @PlanningScore
    private SimpleScore score;

    public List<TestdataEntityRangeBaseEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataEntityRangeBaseEntity> entityList) {
        this.entityList = entityList;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
