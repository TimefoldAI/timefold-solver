package ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.shadow;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.testdomain.DummyVariableListener;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.jspecify.annotations.NonNull;

@PlanningEntity
public class TestdataExtendedShadowedParentEntity extends TestdataObject {

    public static EntityDescriptor<TestdataExtendedShadowedSolution> buildEntityDescriptor() {
        return TestdataExtendedShadowedSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataExtendedShadowedParentEntity.class);
    }

    public static GenuineVariableDescriptor<TestdataExtendedShadowedSolution> buildVariableDescriptorForValue() {
        return buildEntityDescriptor().getGenuineVariableDescriptor("value");
    }

    private TestdataValue value;
    private String firstShadow;
    private String thirdShadow;

    public TestdataExtendedShadowedParentEntity() {
    }

    public TestdataExtendedShadowedParentEntity(String code) {
        super(code);
    }

    public TestdataExtendedShadowedParentEntity(String code, TestdataValue value) {
        this(code);
        this.value = value;
    }

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    public TestdataValue getValue() {
        return value;
    }

    public void setValue(TestdataValue value) {
        this.value = value;
    }

    @ShadowVariable(variableListenerClass = FirstShadowUpdatingVariableListener.class, sourceVariableName = "value")
    public String getFirstShadow() {
        return firstShadow;
    }

    public void setFirstShadow(String firstShadow) {
        this.firstShadow = firstShadow;
    }

    @ShadowVariable(variableListenerClass = ThirdShadowUpdatingVariableListener.class,
            sourceEntityClass = TestdataExtendedShadowedChildEntity.class, sourceVariableName = "secondShadow")
    public String getThirdShadow() {
        return thirdShadow;
    }

    public void setThirdShadow(String thirdShadow) {
        this.thirdShadow = thirdShadow;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    // ************************************************************************
    // Static inner classes
    // ************************************************************************

    public static class FirstShadowUpdatingVariableListener
            extends DummyVariableListener<TestdataExtendedShadowedSolution, TestdataExtendedShadowedParentEntity> {

        @Override
        public void afterEntityAdded(@NonNull ScoreDirector<TestdataExtendedShadowedSolution> scoreDirector,
                @NonNull TestdataExtendedShadowedParentEntity entity) {
            updateShadow(scoreDirector, entity);
        }

        @Override
        public void afterVariableChanged(@NonNull ScoreDirector<TestdataExtendedShadowedSolution> scoreDirector,
                @NonNull TestdataExtendedShadowedParentEntity entity) {
            updateShadow(scoreDirector, entity);
        }

        private void updateShadow(ScoreDirector<TestdataExtendedShadowedSolution> scoreDirector,
                TestdataExtendedShadowedParentEntity entity) {
            TestdataValue value = entity.getValue();
            scoreDirector.beforeVariableChanged(entity, "firstShadow");
            entity.setFirstShadow((value == null) ? null : value.getCode() + "/firstShadow");
            scoreDirector.afterVariableChanged(entity, "firstShadow");
        }

    }

    public static class ThirdShadowUpdatingVariableListener
            implements VariableListener<TestdataExtendedShadowedSolution, TestdataExtendedShadowedChildEntity> {

        @Override
        public void beforeEntityAdded(@NonNull ScoreDirector<TestdataExtendedShadowedSolution> scoreDirector,
                @NonNull TestdataExtendedShadowedChildEntity testdataExtendedShadowedChildEntity) {
            // Do nothing.
        }

        @Override
        public void afterEntityAdded(@NonNull ScoreDirector<TestdataExtendedShadowedSolution> scoreDirector,
                @NonNull TestdataExtendedShadowedChildEntity entity) {
            updateShadow(scoreDirector, entity);
        }

        @Override
        public void beforeVariableChanged(@NonNull ScoreDirector<TestdataExtendedShadowedSolution> scoreDirector,
                @NonNull TestdataExtendedShadowedChildEntity testdataExtendedShadowedChildEntity) {
            // Do nothing.
        }

        @Override
        public void afterVariableChanged(@NonNull ScoreDirector<TestdataExtendedShadowedSolution> scoreDirector,
                @NonNull TestdataExtendedShadowedChildEntity entity) {
            updateShadow(scoreDirector, entity);
        }

        @Override
        public void beforeEntityRemoved(@NonNull ScoreDirector<TestdataExtendedShadowedSolution> scoreDirector,
                @NonNull TestdataExtendedShadowedChildEntity testdataExtendedShadowedChildEntity) {
            // Do nothing.
        }

        @Override
        public void afterEntityRemoved(@NonNull ScoreDirector<TestdataExtendedShadowedSolution> scoreDirector,
                @NonNull TestdataExtendedShadowedChildEntity testdataExtendedShadowedChildEntity) {
            // Do nothing.
        }

        private void updateShadow(ScoreDirector<TestdataExtendedShadowedSolution> scoreDirector,
                TestdataExtendedShadowedChildEntity entity) {
            String secondShadow = entity.getSecondShadow();
            scoreDirector.beforeVariableChanged(entity, "thirdShadow");
            entity.setThirdShadow((secondShadow == null) ? null : secondShadow + "/thirdShadow");
            scoreDirector.afterVariableChanged(entity, "thirdShadow");
        }

    }

}
