package ai.timefold.solver.core.testdomain.shadow.corrupted;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.testdomain.DummyVariableListener;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.jspecify.annotations.NonNull;

@PlanningEntity
public class TestdataCorruptedShadowedEntity extends TestdataObject {

    public static EntityDescriptor<TestdataCorruptedShadowedSolution> buildEntityDescriptor() {
        return TestdataCorruptedShadowedSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataCorruptedShadowedEntity.class);
    }

    private TestdataValue value;
    private Integer count;

    public TestdataCorruptedShadowedEntity(String code) {
        super(code);
    }

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    public TestdataValue getValue() {
        return value;
    }

    public void setValue(TestdataValue value) {
        this.value = value;
    }

    @ShadowVariable(variableListenerClass = CountUpdatingVariableListener.class, sourceVariableName = "value")
    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    // ************************************************************************
    // Static inner classes
    // ************************************************************************

    public static class CountUpdatingVariableListener
            extends DummyVariableListener<TestdataCorruptedShadowedSolution, TestdataCorruptedShadowedEntity> {

        @Override
        public void afterEntityAdded(@NonNull ScoreDirector<TestdataCorruptedShadowedSolution> scoreDirector,
                @NonNull TestdataCorruptedShadowedEntity entity) {
            updateShadow(entity, scoreDirector);
        }

        @Override
        public void afterVariableChanged(@NonNull ScoreDirector<TestdataCorruptedShadowedSolution> scoreDirector,
                @NonNull TestdataCorruptedShadowedEntity entity) {
            updateShadow(entity, scoreDirector);
        }

        private void updateShadow(TestdataCorruptedShadowedEntity entity,
                ScoreDirector<TestdataCorruptedShadowedSolution> scoreDirector) {
            TestdataValue primaryValue = entity.getValue();
            Integer count;
            if (primaryValue == null) {
                count = null;
            } else {
                count = (entity.getCount() == null) ? 0 : entity.getCount();
                count++;
            }
            scoreDirector.beforeVariableChanged(entity, "count");
            entity.setCount(count);
            scoreDirector.afterVariableChanged(entity, "count");
        }

    }

}
