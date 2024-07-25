package ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.piggy_back;

import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.CascadingUpdateShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ListVariableListener;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.api.score.director.ScoreDirector;

@PlanningEntity
public class TestdataInvalidSourceCascadingValue2 {

    @ShadowVariable(sourceEntityClass = TestdataInvalidSourceCascadingEntity.class, sourceVariableName = "valueList",
            variableListenerClass = DummyVariableListener.class)
    private Integer shadowVariable;
    @CascadingUpdateShadowVariable(targetMethodName = "getCascadeValue", sourceVariableName = "shadowVariable")
    private Integer cascadeValue;

    public TestdataInvalidSourceCascadingValue2(Integer cascadeValue) {
        this.cascadeValue = cascadeValue;
    }

    public List<TestdataInvalidSourceCascadingValue2> getValueRange() {
        return null;
    }

    public Integer getCascadeValue() {
        return cascadeValue;
    }

    public void setCascadeValue(Integer cascadeValue) {
        this.cascadeValue = cascadeValue;
    }

    static class DummyVariableListener
            implements
            ListVariableListener<TestdataInvalidSourceCascadingSolution, TestdataInvalidSourceCascadingEntity, TestdataInvalidSourceCascadingValue2> {

        @Override
        public void afterListVariableElementUnassigned(ScoreDirector<TestdataInvalidSourceCascadingSolution> scoreDirector,
                TestdataInvalidSourceCascadingValue2 testdataInvalidSourceCascadingValue2) {
            // Do nothing
        }

        @Override
        public void beforeListVariableChanged(ScoreDirector<TestdataInvalidSourceCascadingSolution> scoreDirector,
                TestdataInvalidSourceCascadingEntity testdataInvalidSourceCascadingEntity, int fromIndex, int toIndex) {
            // Do nothing
        }

        @Override
        public void afterListVariableChanged(ScoreDirector<TestdataInvalidSourceCascadingSolution> scoreDirector,
                TestdataInvalidSourceCascadingEntity testdataInvalidSourceCascadingEntity, int fromIndex, int toIndex) {
            // Do nothing
        }

        @Override
        public void beforeEntityAdded(ScoreDirector<TestdataInvalidSourceCascadingSolution> scoreDirector,
                TestdataInvalidSourceCascadingEntity testdataInvalidSourceCascadingEntity) {
            // Do nothing
        }

        @Override
        public void afterEntityAdded(ScoreDirector<TestdataInvalidSourceCascadingSolution> scoreDirector,
                TestdataInvalidSourceCascadingEntity testdataInvalidSourceCascadingEntity) {
            // Do nothing
        }

        @Override
        public void beforeEntityRemoved(ScoreDirector<TestdataInvalidSourceCascadingSolution> scoreDirector,
                TestdataInvalidSourceCascadingEntity testdataInvalidSourceCascadingEntity) {
            // Do nothing
        }

        @Override
        public void afterEntityRemoved(ScoreDirector<TestdataInvalidSourceCascadingSolution> scoreDirector,
                TestdataInvalidSourceCascadingEntity testdataInvalidSourceCascadingEntity) {
            // Do nothing
        }
    }
}
