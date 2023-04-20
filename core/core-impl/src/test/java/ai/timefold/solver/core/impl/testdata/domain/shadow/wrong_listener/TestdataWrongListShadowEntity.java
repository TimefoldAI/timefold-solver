package ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_listener;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListValue;

@PlanningEntity
public class TestdataWrongListShadowEntity {

    public static EntityDescriptor<TestdataListSolution> buildEntityDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataListSolution.class,
                TestdataListEntity.class,
                TestdataWrongListShadowEntity.class,
                TestdataListValue.class).findEntityDescriptorOrFail(TestdataWrongListShadowEntity.class);
    }

    @ShadowVariable(variableListenerClass = MyBasicVariableListener.class,
            sourceEntityClass = TestdataListEntity.class, sourceVariableName = "valueList")
    private String shadow;

    public String getShadow() {
        return shadow;
    }

    public void setShadow(String shadow) {
        this.shadow = shadow;
    }

    public static class MyBasicVariableListener implements VariableListener<TestdataListSolution, TestdataListEntity> {

        @Override
        public void beforeEntityAdded(ScoreDirector<TestdataListSolution> scoreDirector, TestdataListEntity entity) {
        }

        @Override
        public void afterEntityAdded(ScoreDirector<TestdataListSolution> scoreDirector, TestdataListEntity entity) {
        }

        @Override
        public void beforeEntityRemoved(ScoreDirector<TestdataListSolution> scoreDirector, TestdataListEntity entity) {
        }

        @Override
        public void afterEntityRemoved(ScoreDirector<TestdataListSolution> scoreDirector, TestdataListEntity entity) {
        }

        @Override
        public void beforeVariableChanged(ScoreDirector<TestdataListSolution> scoreDirector, TestdataListEntity entity) {
        }

        @Override
        public void afterVariableChanged(ScoreDirector<TestdataListSolution> scoreDirector, TestdataListEntity entity) {
        }
    }
}
