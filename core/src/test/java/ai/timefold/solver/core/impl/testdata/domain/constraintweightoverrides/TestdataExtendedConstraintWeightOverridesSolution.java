package ai.timefold.solver.core.impl.testdata.domain.constraintweightoverrides;

import java.util.ArrayList;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

@PlanningSolution
public class TestdataExtendedConstraintWeightOverridesSolution extends TestdataConstraintWeightOverridesSolution {

    public static SolutionDescriptor<TestdataExtendedConstraintWeightOverridesSolution> buildExtendedSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataExtendedConstraintWeightOverridesSolution.class,
                TestdataEntity.class);
    }

    public static TestdataExtendedConstraintWeightOverridesSolution generateExtendedSolution(int valueListSize,
            int entityListSize) {
        var solution = new TestdataExtendedConstraintWeightOverridesSolution("Generated Solution 0");
        var valueList = new ArrayList<TestdataValue>(valueListSize);
        for (var i = 0; i < valueListSize; i++) {
            var value = new TestdataValue("Generated Value " + i);
            valueList.add(value);
        }
        solution.setValueList(valueList);
        var entityList = new ArrayList<TestdataEntity>(entityListSize);
        for (var i = 0; i < entityListSize; i++) {
            var value = valueList.get(i % valueListSize);
            var entity = new TestdataEntity("Generated Entity " + i, value);
            entityList.add(entity);
        }
        solution.setEntityList(entityList);
        solution.setConstraintWeightOverrides(ConstraintWeightOverrides.none());
        return solution;
    }

    private ConstraintWeightOverrides<SimpleScore> constraintWeightOverrides;

    public TestdataExtendedConstraintWeightOverridesSolution() {
    }

    public TestdataExtendedConstraintWeightOverridesSolution(String code) {
        super(code);
    }

    @Override
    public ConstraintWeightOverrides<SimpleScore> getConstraintWeightOverrides() {
        return constraintWeightOverrides;
    }

    @Override
    public void setConstraintWeightOverrides(ConstraintWeightOverrides<SimpleScore> constraintWeightOverrides) {
        this.constraintWeightOverrides = constraintWeightOverrides;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

}
