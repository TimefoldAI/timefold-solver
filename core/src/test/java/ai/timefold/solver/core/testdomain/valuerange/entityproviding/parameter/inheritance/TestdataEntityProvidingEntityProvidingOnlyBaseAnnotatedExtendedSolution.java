package ai.timefold.solver.core.testdomain.valuerange.entityproviding.parameter.inheritance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.TestdataValue;

public class TestdataEntityProvidingEntityProvidingOnlyBaseAnnotatedExtendedSolution extends TestdataEntityProvidingOnlyBaseAnnotatedSolution {

    public static SolutionDescriptor<TestdataEntityProvidingEntityProvidingOnlyBaseAnnotatedExtendedSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataEntityProvidingEntityProvidingOnlyBaseAnnotatedExtendedSolution.class,
                TestdataEntityProvidingOnlyBaseAnnotatedBaseEntity.class, TestdataEntityProvidingOnlyBaseAnnotatedChildEntity.class);
    }

    public static TestdataEntityProvidingEntityProvidingOnlyBaseAnnotatedExtendedSolution generateSolution(int valueListSize, int entityListSize) {
        var solution = new TestdataEntityProvidingEntityProvidingOnlyBaseAnnotatedExtendedSolution("s1");
        var valueList = new ArrayList<TestdataValue>(valueListSize);
        for (int i = 0; i < valueListSize; i++) {
            valueList.add(new TestdataValue("Generated Value " + i));
        }
        solution.setValueList(valueList);
        var entityList = new ArrayList<TestdataEntityProvidingOnlyBaseAnnotatedChildEntity>(entityListSize);
        var idx = 0;
        for (int i = 0; i < entityListSize; i++) {
            var entity = new TestdataEntityProvidingOnlyBaseAnnotatedChildEntity(String.valueOf(idx++));
            entityList.add(entity);
        }
        solution.setEntityList(entityList);
        solution.setConstraintWeightOverrides(ConstraintWeightOverrides.<SimpleScore> of(Collections.EMPTY_MAP));
        return solution;
    }

    private Object extraObject;

    public TestdataEntityProvidingEntityProvidingOnlyBaseAnnotatedExtendedSolution() {
    }

    public TestdataEntityProvidingEntityProvidingOnlyBaseAnnotatedExtendedSolution(String code) {
        super(code);
    }

    public TestdataEntityProvidingEntityProvidingOnlyBaseAnnotatedExtendedSolution(String code, Object extraObject) {
        super(code);
        this.extraObject = extraObject;
    }

    @Override
    public List<TestdataEntityProvidingOnlyBaseAnnotatedChildEntity> getEntityList() {
        return (List<TestdataEntityProvidingOnlyBaseAnnotatedChildEntity>) super.getEntityList();
    }

    public Object getExtraObject() {
        return extraObject;
    }

    public void setExtraObject(Object extraObject) {
        this.extraObject = extraObject;
    }
}
