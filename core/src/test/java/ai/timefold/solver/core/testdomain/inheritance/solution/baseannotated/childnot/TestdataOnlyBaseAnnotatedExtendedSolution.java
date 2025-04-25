package ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childnot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.TestdataValue;

public class TestdataOnlyBaseAnnotatedExtendedSolution extends TestdataOnlyBaseAnnotatedSolution {

    public static SolutionDescriptor<TestdataOnlyBaseAnnotatedExtendedSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataOnlyBaseAnnotatedExtendedSolution.class,
                TestdataOnlyBaseAnnotatedBaseEntity.class, TestdataOnlyBaseAnnotatedChildEntity.class);
    }

    public static TestdataOnlyBaseAnnotatedExtendedSolution generateSolution(int valueListSize, int entityListSize) {
        var solution = new TestdataOnlyBaseAnnotatedExtendedSolution("s1");
        var valueList = new ArrayList<TestdataValue>(valueListSize);
        for (int i = 0; i < valueListSize; i++) {
            valueList.add(new TestdataValue("Generated Value " + i));
        }
        solution.setValueList(valueList);
        var entityList = new ArrayList<TestdataOnlyBaseAnnotatedChildEntity>(entityListSize);
        var idx = 0;
        for (int i = 0; i < entityListSize; i++) {
            var entity = new TestdataOnlyBaseAnnotatedChildEntity(String.valueOf(idx++));
            entityList.add(entity);
        }
        solution.setEntityList(entityList);
        solution.setConstraintWeightOverrides(ConstraintWeightOverrides.<SimpleScore> of(Collections.EMPTY_MAP));
        return solution;
    }

    private Object extraObject;

    public TestdataOnlyBaseAnnotatedExtendedSolution() {
    }

    public TestdataOnlyBaseAnnotatedExtendedSolution(String code) {
        super(code);
    }

    public TestdataOnlyBaseAnnotatedExtendedSolution(String code, Object extraObject) {
        super(code);
        this.extraObject = extraObject;
    }

    @Override
    public List<TestdataOnlyBaseAnnotatedChildEntity> getEntityList() {
        return (List<TestdataOnlyBaseAnnotatedChildEntity>) super.getEntityList();
    }

    public Object getExtraObject() {
        return extraObject;
    }

    public void setExtraObject(Object extraObject) {
        this.extraObject = extraObject;
    }
}
