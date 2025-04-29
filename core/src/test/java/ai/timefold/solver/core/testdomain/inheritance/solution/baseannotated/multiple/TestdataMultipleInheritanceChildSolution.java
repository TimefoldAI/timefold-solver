package ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.multiple;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;

@PlanningSolution
public class TestdataMultipleInheritanceChildSolution extends TestdataMultipleInheritanceBaseSolution {

    @PlanningEntityCollectionProperty
    private List<? extends TestdataMultipleInheritanceEntity> entityList;
    @PlanningScore
    private SimpleScore score;

    public List<? extends TestdataMultipleInheritanceEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<? extends TestdataMultipleInheritanceEntity> entityList) {
        this.entityList = entityList;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
