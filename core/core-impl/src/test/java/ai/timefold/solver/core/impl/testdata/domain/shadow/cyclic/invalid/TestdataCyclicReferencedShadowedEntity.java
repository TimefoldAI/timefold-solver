package ai.timefold.solver.core.impl.testdata.domain.shadow.cyclic.invalid;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PiggybackShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.DummyVariableListener;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

@PlanningEntity
public class TestdataCyclicReferencedShadowedEntity extends TestdataObject {

    public static EntityDescriptor<TestdataCyclicReferencedShadowedSolution> buildEntityDescriptor() {
        return TestdataCyclicReferencedShadowedSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataCyclicReferencedShadowedEntity.class);
    }

    public static GenuineVariableDescriptor<TestdataCyclicReferencedShadowedSolution> buildVariableDescriptorForValue() {
        return buildEntityDescriptor().getGenuineVariableDescriptor("value");
    }

    private TestdataValue value;
    private boolean barber;
    private boolean cutsOwnHair;

    public TestdataCyclicReferencedShadowedEntity() {
    }

    public TestdataCyclicReferencedShadowedEntity(String code) {
        super(code);
    }

    public TestdataCyclicReferencedShadowedEntity(String code, TestdataValue value) {
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

    @ShadowVariable(variableListenerClass = BarberAndCutsOwnHairUpdatingVariableListener.class, sourceVariableName = "value")
    @ShadowVariable(variableListenerClass = BarberAndCutsOwnHairUpdatingVariableListener.class,
            sourceVariableName = "cutsOwnHair")
    public boolean isBarber() {
        return barber;
    }

    public void setBarber(boolean barber) {
        this.barber = barber;
    }

    @PiggybackShadowVariable(shadowVariableName = "barber")
    public boolean isCutsOwnHair() {
        return cutsOwnHair;
    }

    public void setCutsOwnHair(boolean cutsOwnHair) {
        this.cutsOwnHair = cutsOwnHair;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    // ************************************************************************
    // Static inner classes
    // ************************************************************************

    public static class BarberAndCutsOwnHairUpdatingVariableListener
            extends DummyVariableListener<TestdataCyclicReferencedShadowedSolution, TestdataCyclicReferencedShadowedEntity> {

        @Override
        public void afterEntityAdded(ScoreDirector<TestdataCyclicReferencedShadowedSolution> scoreDirector,
                TestdataCyclicReferencedShadowedEntity entity) {
            updateShadow(entity, scoreDirector);
        }

        @Override
        public void afterVariableChanged(ScoreDirector<TestdataCyclicReferencedShadowedSolution> scoreDirector,
                TestdataCyclicReferencedShadowedEntity entity) {
            updateShadow(entity, scoreDirector);
        }

        private void updateShadow(TestdataCyclicReferencedShadowedEntity entity,
                ScoreDirector<TestdataCyclicReferencedShadowedSolution> scoreDirector) {
            // The barber cuts the hair of everyone in the village who does not cut his/her own hair
            // Does the barber cut his own hair?
            TestdataValue value = entity.getValue();
            boolean barber = !entity.isCutsOwnHair();
            scoreDirector.beforeVariableChanged(entity, "barber");
            entity.setBarber(value != null && barber);
            scoreDirector.afterVariableChanged(entity, "barber");
            scoreDirector.beforeVariableChanged(entity, "cutsOwnHair");
            entity.setCutsOwnHair(value != null && !barber);
            scoreDirector.afterVariableChanged(entity, "cutsOwnHair");
        }

    }

}
