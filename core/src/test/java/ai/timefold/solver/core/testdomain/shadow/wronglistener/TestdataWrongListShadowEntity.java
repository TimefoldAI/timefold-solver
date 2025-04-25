package ai.timefold.solver.core.testdomain.shadow.wronglistener;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;

import org.jspecify.annotations.NonNull;

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
        public void beforeEntityAdded(@NonNull ScoreDirector<TestdataListSolution> scoreDirector,
                @NonNull TestdataListEntity entity) {
            // Ignore
        }

        @Override
        public void afterEntityAdded(@NonNull ScoreDirector<TestdataListSolution> scoreDirector,
                @NonNull TestdataListEntity entity) {
            // Ignore
        }

        @Override
        public void beforeEntityRemoved(@NonNull ScoreDirector<TestdataListSolution> scoreDirector,
                @NonNull TestdataListEntity entity) {
            // Ignore
        }

        @Override
        public void afterEntityRemoved(@NonNull ScoreDirector<TestdataListSolution> scoreDirector,
                @NonNull TestdataListEntity entity) {
            // Ignore
        }

        @Override
        public void beforeVariableChanged(@NonNull ScoreDirector<TestdataListSolution> scoreDirector,
                @NonNull TestdataListEntity entity) {
            // Ignore
        }

        @Override
        public void afterVariableChanged(@NonNull ScoreDirector<TestdataListSolution> scoreDirector,
                @NonNull TestdataListEntity entity) {
            // Ignore
        }
    }
}
