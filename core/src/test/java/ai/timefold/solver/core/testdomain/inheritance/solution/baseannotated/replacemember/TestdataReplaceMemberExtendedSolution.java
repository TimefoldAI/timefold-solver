package ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.replacemember;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;

@PlanningSolution
public class TestdataReplaceMemberExtendedSolution extends TestdataReplaceMemberSolution {

    public static TestdataReplaceMemberExtendedSolution generateSolution(int valueListSize, int entityListSize) {
        var solution = new TestdataReplaceMemberExtendedSolution();
        var valueList = new ArrayList<String>(valueListSize);
        for (int i = 0; i < valueListSize; i++) {
            valueList.add("Generated Value " + i);
        }
        solution.setValueList(valueList);
        var entityList = new ArrayList<TestdataReplaceMemberEntity>(entityListSize);
        var idx = 0;
        for (int i = 0; i < entityListSize; i++) {
            var entity = new TestdataReplaceMemberEntity(idx++);
            entityList.add(entity);
        }
        solution.setEntityList(entityList);
        solution.setConstraintWeightOverrides(ConstraintWeightOverrides.<SimpleScore> of(Collections.EMPTY_MAP));
        return solution;
    }

    @PlanningEntityCollectionProperty
    private List<? extends TestdataReplaceMemberEntity> entityList;

    @Override
    public List<? extends TestdataReplaceMemberEntity> getEntityList() {
        return entityList;
    }

    @Override
    public void setEntityList(List<? extends TestdataReplaceMemberEntity> entityList) {
        this.entityList = entityList;
    }
}
