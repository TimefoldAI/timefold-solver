package ai.timefold.solver.core.impl.domain.variable.custom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.shadow.TestdataExtendedShadowedChildEntity;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.shadow.TestdataExtendedShadowedParentEntity;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.shadow.TestdataExtendedShadowedSolution;
import ai.timefold.solver.core.testdomain.shadow.cyclic.TestdataSevenNonCyclicShadowedSolution;
import ai.timefold.solver.core.testdomain.shadow.cyclic.invalid.TestdataCyclicReferencedShadowedSolution;
import ai.timefold.solver.core.testdomain.shadow.cyclic.invalid.TestdataCyclicShadowedSolution;
import ai.timefold.solver.core.testdomain.shadow.manytomany.TestdataManyToManyShadowedEntity;
import ai.timefold.solver.core.testdomain.shadow.manytomany.TestdataManyToManyShadowedEntityUniqueEvents;
import ai.timefold.solver.core.testdomain.shadow.manytomany.TestdataManyToManyShadowedSolution;
import ai.timefold.solver.core.testdomain.shadow.wronglistener.TestdataWrongBasicShadowEntity;
import ai.timefold.solver.core.testdomain.shadow.wronglistener.TestdataWrongListShadowEntity;
import ai.timefold.solver.core.testutil.PlannerTestUtils;

import org.junit.jupiter.api.Test;

class CustomVariableListenerTest {

    @Test
    void cyclic() {
        assertThatIllegalStateException().isThrownBy(TestdataCyclicShadowedSolution::buildSolutionDescriptor);
    }

    @Test
    void cyclicReferenced() {
        assertThatIllegalStateException().isThrownBy(TestdataCyclicReferencedShadowedSolution::buildSolutionDescriptor);
    }

    @Test
    void nonCyclicWithSevenDisorderedShadows() {
        assertThatCode(TestdataSevenNonCyclicShadowedSolution::buildSolutionDescriptor)
                .doesNotThrowAnyException();
    }

    @Test
    void listVariableListenerWithBasicSourceVariable() {
        assertThatIllegalArgumentException().isThrownBy(TestdataWrongBasicShadowEntity::buildEntityDescriptor)
                .withMessageContaining("basic variable");
    }

    @Test
    void basicVariableListenerWithListSourceVariable() {
        assertThatIllegalArgumentException().isThrownBy(TestdataWrongListShadowEntity::buildEntityDescriptor)
                .withMessageContaining("list variable");
    }

    @Test
    void extendedZigZag() {
        var variableDescriptor = TestdataExtendedShadowedParentEntity.buildVariableDescriptorForValue();
        var scoreDirector =
                PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());

        var val1 = new TestdataValue("1");
        var val2 = new TestdataValue("2");
        var val3 = new TestdataValue("3");
        var a = new TestdataExtendedShadowedParentEntity("a", null);
        var b = new TestdataExtendedShadowedParentEntity("b", null);
        var c = new TestdataExtendedShadowedChildEntity("c", null);

        var solution = new TestdataExtendedShadowedSolution("solution");
        solution.setEntityList(Arrays.asList(a, b, c));
        solution.setValueList(Arrays.asList(val1, val2, val3));
        scoreDirector.setWorkingSolution(solution);

        scoreDirector.beforeVariableChanged(variableDescriptor, a);
        a.setValue(val1);
        scoreDirector.afterVariableChanged(variableDescriptor, a);
        scoreDirector.triggerVariableListeners();
        assertThat(a.getFirstShadow()).isEqualTo("1/firstShadow");
        assertThat(a.getThirdShadow()).isNull();

        scoreDirector.beforeVariableChanged(variableDescriptor, a);
        a.setValue(val3);
        scoreDirector.afterVariableChanged(variableDescriptor, a);
        scoreDirector.triggerVariableListeners();
        assertThat(a.getFirstShadow()).isEqualTo("3/firstShadow");
        assertThat(a.getThirdShadow()).isNull();

        scoreDirector.beforeVariableChanged(variableDescriptor, c);
        c.setValue(val1);
        scoreDirector.afterVariableChanged(variableDescriptor, c);
        scoreDirector.triggerVariableListeners();
        assertThat(c.getFirstShadow()).isEqualTo("1/firstShadow");
        assertThat(c.getSecondShadow()).isEqualTo("1/firstShadow/secondShadow");
        assertThat(c.getThirdShadow()).isEqualTo("1/firstShadow/secondShadow/thirdShadow");

        scoreDirector.beforeVariableChanged(variableDescriptor, c);
        c.setValue(val3);
        scoreDirector.afterVariableChanged(variableDescriptor, c);
        scoreDirector.triggerVariableListeners();
        assertThat(c.getFirstShadow()).isEqualTo("3/firstShadow");
        assertThat(c.getSecondShadow()).isEqualTo("3/firstShadow/secondShadow");
        assertThat(c.getThirdShadow()).isEqualTo("3/firstShadow/secondShadow/thirdShadow");
    }

    @Test
    void manyToMany() {
        var entityDescriptor = TestdataManyToManyShadowedEntity.buildEntityDescriptor();
        var primaryVariableDescriptor = entityDescriptor.getGenuineVariableDescriptor("primaryValue");
        var secondaryVariableDescriptor = entityDescriptor.getGenuineVariableDescriptor("secondaryValue");
        var scoreDirector =
                PlannerTestUtils.mockScoreDirector(primaryVariableDescriptor.getEntityDescriptor().getSolutionDescriptor());

        var val1 = new TestdataValue("1");
        var val2 = new TestdataValue("2");
        var val3 = new TestdataValue("3");
        var val4 = new TestdataValue("4");
        var a = new TestdataManyToManyShadowedEntity("a", null, null);
        var b = new TestdataManyToManyShadowedEntity("b", null, null);
        var c = new TestdataManyToManyShadowedEntity("c", null, null);

        var solution = new TestdataManyToManyShadowedSolution("solution");
        solution.setEntityList(Arrays.asList(a, b, c));
        solution.setValueList(Arrays.asList(val1, val2, val3, val4));
        scoreDirector.setWorkingSolution(solution);

        scoreDirector.beforeVariableChanged(primaryVariableDescriptor, a);
        a.setPrimaryValue(val1);
        scoreDirector.afterVariableChanged(primaryVariableDescriptor, a);
        scoreDirector.triggerVariableListeners();
        assertThat(a.getComposedCode()).isNull();
        assertThat(a.getReverseComposedCode()).isNull();

        scoreDirector.beforeVariableChanged(secondaryVariableDescriptor, a);
        a.setSecondaryValue(val3);
        scoreDirector.afterVariableChanged(secondaryVariableDescriptor, a);
        scoreDirector.triggerVariableListeners();
        assertThat(a.getComposedCode()).isEqualTo("1-3");
        assertThat(a.getReverseComposedCode()).isEqualTo("3-1");

        scoreDirector.beforeVariableChanged(secondaryVariableDescriptor, a);
        a.setSecondaryValue(val4);
        scoreDirector.afterVariableChanged(secondaryVariableDescriptor, a);
        scoreDirector.triggerVariableListeners();
        assertThat(a.getComposedCode()).isEqualTo("1-4");
        assertThat(a.getReverseComposedCode()).isEqualTo("4-1");

        scoreDirector.beforeVariableChanged(primaryVariableDescriptor, a);
        a.setPrimaryValue(val2);
        scoreDirector.afterVariableChanged(primaryVariableDescriptor, a);
        scoreDirector.triggerVariableListeners();
        assertThat(a.getComposedCode()).isEqualTo("2-4");
        assertThat(a.getReverseComposedCode()).isEqualTo("4-2");

        scoreDirector.beforeVariableChanged(primaryVariableDescriptor, a);
        a.setPrimaryValue(null);
        scoreDirector.afterVariableChanged(primaryVariableDescriptor, a);
        scoreDirector.triggerVariableListeners();
        assertThat(a.getComposedCode()).isNull();
        assertThat(a.getReverseComposedCode()).isNull();

        scoreDirector.beforeVariableChanged(primaryVariableDescriptor, c);
        c.setPrimaryValue(val1);
        scoreDirector.afterVariableChanged(primaryVariableDescriptor, c);
        scoreDirector.beforeVariableChanged(secondaryVariableDescriptor, c);
        c.setSecondaryValue(val3);
        scoreDirector.afterVariableChanged(secondaryVariableDescriptor, c);
        scoreDirector.triggerVariableListeners();
        assertThat(c.getComposedCode()).isEqualTo("1-3");
        assertThat(c.getReverseComposedCode()).isEqualTo("3-1");
    }

    @Test
    void manyToManyRequiresUniqueEntityEvents() {
        var entityDescriptor = TestdataManyToManyShadowedEntityUniqueEvents.buildEntityDescriptor();
        var primaryVariableDescriptor = entityDescriptor.getGenuineVariableDescriptor("primaryValue");
        var secondaryVariableDescriptor = entityDescriptor.getGenuineVariableDescriptor("secondaryValue");
        var scoreDirector = PlannerTestUtils.mockScoreDirector(entityDescriptor.getSolutionDescriptor());

        var val1 = new TestdataValue("1");
        var b = new TestdataManyToManyShadowedEntityUniqueEvents("b", null, null);
        var solution = new TestdataManyToManyShadowedSolution("solution");
        solution.setEntityList(new ArrayList<>());
        solution.setValueList(List.of(val1));
        scoreDirector.setWorkingSolution(solution);
        scoreDirector.forceTriggerVariableListeners();

        scoreDirector.beforeEntityAdded(b);
        scoreDirector.getWorkingSolution().getEntityList().add(b);
        scoreDirector.afterEntityAdded(b);
        scoreDirector.beforeVariableChanged(primaryVariableDescriptor, b);
        b.setPrimaryValue(val1);
        scoreDirector.afterVariableChanged(primaryVariableDescriptor, b);
        scoreDirector.beforeVariableChanged(secondaryVariableDescriptor, b);
        b.setSecondaryValue(val1);
        scoreDirector.afterVariableChanged(secondaryVariableDescriptor, b);
        scoreDirector.triggerVariableListeners();

        verify(scoreDirector).setWorkingSolution(solution);
        verify(scoreDirector).beforeEntityAdded(b);
        verify(scoreDirector).afterEntityAdded(b);
        verify(scoreDirector).beforeVariableChanged(primaryVariableDescriptor, b);
        verify(scoreDirector).afterVariableChanged(primaryVariableDescriptor, b);
        verify(scoreDirector).beforeVariableChanged(secondaryVariableDescriptor, b);
        verify(scoreDirector).afterVariableChanged(secondaryVariableDescriptor, b);
        verify(scoreDirector).triggerVariableListeners();

        // The 1st element is caused by afterEntityAdded(). It is unique because it's the only one of the "EntityAdded" type.
        // The 2nd element is caused by the first afterVariableChanged() event.
        // The second afterVariableChangedEvent was deduplicated.
        assertThat(b.getComposedCodeLog()).containsExactly("1-1", "1-1");
    }

}
