package ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseannotated.childnot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;

public class TestdataOnlyBaseAnnotatedExtendedSolution extends TestdataOnlyBaseAnnotatedSolution {

    public static TestdataOnlyBaseAnnotatedExtendedSolution generateSolution(int valueListSize, int entityListSize) {
        var solution = new TestdataOnlyBaseAnnotatedExtendedSolution();
        var valueList = new ArrayList<String>(valueListSize);
        for (int i = 0; i < valueListSize; i++) {
            valueList.add("Generated Value " + i);
        }
        solution.setValueList(valueList);
        var entityList = new ArrayList<TestdataOnlyBaseAnnotatedChildEntity>(entityListSize);
        var idx = 0;
        for (int i = 0; i < entityListSize; i++) {
            var entity = new TestdataOnlyBaseAnnotatedChildEntity(idx++);
            entityList.add(entity);
        }
        solution.setEntityList(entityList);
        solution.setConstraintWeightOverrides(ConstraintWeightOverrides.<SimpleScore> of(Collections.EMPTY_MAP));
        return solution;
    }

    @Override
    public List<TestdataOnlyBaseAnnotatedChildEntity> getEntityList() {
        return (List<TestdataOnlyBaseAnnotatedChildEntity>) super.getEntityList();
    }
}
