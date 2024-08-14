package ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_listener;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.ListVariableListener;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.jspecify.annotations.NonNull;

@PlanningEntity
public class TestdataWrongBasicShadowEntity {

    public static EntityDescriptor<TestdataSolution> buildEntityDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataSolution.class,
                TestdataEntity.class,
                TestdataWrongBasicShadowEntity.class).findEntityDescriptorOrFail(TestdataWrongBasicShadowEntity.class);
    }

    @ShadowVariable(variableListenerClass = MyListVariableListener.class,
            sourceEntityClass = TestdataEntity.class, sourceVariableName = "value")
    private String shadow;

    public String getShadow() {
        return shadow;
    }

    public void setShadow(String shadow) {
        this.shadow = shadow;
    }

    public static class MyListVariableListener implements ListVariableListener<TestdataSolution, TestdataEntity, Object> {

        @Override
        public void beforeEntityAdded(@NonNull ScoreDirector<TestdataSolution> scoreDirector, @NonNull TestdataEntity entity) {
        }

        @Override
        public void afterEntityAdded(@NonNull ScoreDirector<TestdataSolution> scoreDirector, @NonNull TestdataEntity entity) {
        }

        @Override
        public void beforeEntityRemoved(@NonNull ScoreDirector<TestdataSolution> scoreDirector,
                @NonNull TestdataEntity entity) {
        }

        @Override
        public void afterEntityRemoved(@NonNull ScoreDirector<TestdataSolution> scoreDirector, @NonNull TestdataEntity entity) {
        }

        @Override
        public void afterListVariableElementUnassigned(@NonNull ScoreDirector<TestdataSolution> scoreDirector,
                @NonNull Object o) {
        }

        @Override
        public void beforeListVariableChanged(@NonNull ScoreDirector<TestdataSolution> scoreDirector,
                @NonNull TestdataEntity entity,
                int fromIndex, int toIndex) {
        }

        @Override
        public void afterListVariableChanged(@NonNull ScoreDirector<TestdataSolution> scoreDirector,
                @NonNull TestdataEntity entity,
                int fromIndex, int toIndex) {
        }
    }
}
