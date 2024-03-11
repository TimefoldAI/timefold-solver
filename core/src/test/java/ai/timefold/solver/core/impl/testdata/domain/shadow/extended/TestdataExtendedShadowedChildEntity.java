package ai.timefold.solver.core.impl.testdata.domain.shadow.extended;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.DummyVariableListener;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

@PlanningEntity
public class TestdataExtendedShadowedChildEntity extends TestdataExtendedShadowedParentEntity {

    public static EntityDescriptor<TestdataExtendedShadowedSolution> buildEntityDescriptor() {
        return TestdataExtendedShadowedSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataExtendedShadowedChildEntity.class);
    }

    private String secondShadow;

    public TestdataExtendedShadowedChildEntity() {
    }

    public TestdataExtendedShadowedChildEntity(String code) {
        super(code);
    }

    public TestdataExtendedShadowedChildEntity(String code, TestdataValue value) {
        super(code, value);
    }

    @ShadowVariable(variableListenerClass = SecondShadowUpdatingVariableListener.class, sourceVariableName = "firstShadow")
    public String getSecondShadow() {
        return secondShadow;
    }

    public void setSecondShadow(String secondShadow) {
        this.secondShadow = secondShadow;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    // ************************************************************************
    // Static inner classes
    // ************************************************************************

    public static class SecondShadowUpdatingVariableListener
            extends DummyVariableListener<TestdataExtendedShadowedSolution, TestdataExtendedShadowedParentEntity> {

        @Override
        public void afterEntityAdded(ScoreDirector<TestdataExtendedShadowedSolution> scoreDirector,
                TestdataExtendedShadowedParentEntity entity) {
            updateShadow(scoreDirector, entity);
        }

        @Override
        public void afterVariableChanged(ScoreDirector<TestdataExtendedShadowedSolution> scoreDirector,
                TestdataExtendedShadowedParentEntity entity) {
            updateShadow(scoreDirector, entity);
        }

        private void updateShadow(ScoreDirector<TestdataExtendedShadowedSolution> scoreDirector,
                TestdataExtendedShadowedParentEntity entity) {
            String firstShadow = entity.getFirstShadow();
            if (entity instanceof TestdataExtendedShadowedChildEntity childEntity) {
                scoreDirector.beforeVariableChanged(childEntity, "secondShadow");
                childEntity.setSecondShadow((firstShadow == null) ? null : firstShadow + "/secondShadow");
                scoreDirector.afterVariableChanged(childEntity, "secondShadow");
            }
        }

    }

}
