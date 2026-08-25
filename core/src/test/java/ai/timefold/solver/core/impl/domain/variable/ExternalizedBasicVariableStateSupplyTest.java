package ai.timefold.solver.core.impl.domain.variable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

import ai.timefold.solver.core.impl.domain.variable.inverserelation.InverseRelationShadowVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.shadow.inverserelation.TestdataInverseRelationEntity;
import ai.timefold.solver.core.testdomain.shadow.inverserelation.TestdataInverseRelationSolution;
import ai.timefold.solver.core.testdomain.shadow.inverserelation.TestdataInverseRelationValue;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ExternalizedBasicVariableStateSupplyTest {

    @Test
    void externalizedMode() {
        var scoreDirector = mock(InnerScoreDirector.class);
        var solutionDescriptor = TestdataInverseRelationSolution.buildSolutionDescriptor();
        var entityDescriptor = solutionDescriptor.findEntityDescriptorOrFail(TestdataInverseRelationEntity.class);
        var shadowEntityDescriptor = solutionDescriptor.findEntityDescriptorOrFail(TestdataInverseRelationValue.class);
        var entitiesVariableDescriptor =
                (InverseRelationShadowVariableDescriptor<TestdataInverseRelationSolution>) shadowEntityDescriptor
                        .getShadowVariableDescriptor("entities");
        @SuppressWarnings("unchecked")
        var notifier = (Consumer<Object>) mock(Consumer.class);
        var supply = new ExternalizedBasicVariableStateSupply<>(entityDescriptor.getGenuineVariableDescriptor("value"),
                notifier);
        supply.externalize(entitiesVariableDescriptor);

        var val1 = new TestdataInverseRelationValue("1");
        var val2 = new TestdataInverseRelationValue("2");
        var val3 = new TestdataInverseRelationValue("3");
        var a = new TestdataInverseRelationEntity("a", val1);
        var b = new TestdataInverseRelationEntity("b", val1);
        var c = new TestdataInverseRelationEntity("c", val3);
        var d = new TestdataInverseRelationEntity("d", val3);

        var solution = new TestdataInverseRelationSolution("solution");
        solution.setEntityList(Arrays.asList(a, b, c, d));
        solution.setValueList(Arrays.asList(val1, val2, val3));

        assertThat(val1.getEntities()).containsExactly(a, b);
        assertThat(val2.getEntities()).isEmpty();
        assertThat(val3.getEntities()).containsExactly(c, d);

        supply.beforeVariableChanged(scoreDirector, c);
        c.setValue(val2);
        supply.afterVariableChanged(scoreDirector, c);

        assertThat(val1.getEntities()).containsExactly(a, b);
        assertThat(val2.getEntities()).containsExactly(c);
        assertThat(val3.getEntities()).containsExactly(d);
    }

    @Test
    void mapMode() {
        var variableDescriptor = TestdataEntity.buildVariableDescriptorForValue();
        var scoreDirector = mock(InnerScoreDirector.class);
        @SuppressWarnings("unchecked")
        var notifier = (Consumer<Object>) mock(Consumer.class);
        var supply = new ExternalizedBasicVariableStateSupply<>(variableDescriptor, notifier);

        var val1 = new TestdataValue("1");
        var val2 = new TestdataValue("2");
        var val3 = new TestdataValue("3");
        var a = new TestdataEntity("a", val1);
        var b = new TestdataEntity("b", val1);
        var c = new TestdataEntity("c", val3);
        var d = new TestdataEntity("d", val3);

        var solution = new TestdataSolution("solution");
        solution.setEntityList(Arrays.asList(a, b, c, d));
        solution.setValueList(Arrays.asList(val1, val2, val3));

        when(scoreDirector.getWorkingSolution()).thenReturn(solution);
        supply.resetWorkingSolution(scoreDirector);

        assertThat((Collection<TestdataEntity>) supply.getInverseCollection(val1)).containsExactlyInAnyOrder(a, b);
        assertThat((Collection<TestdataEntity>) supply.getInverseCollection(val2)).isEmpty();
        assertThat((Collection<TestdataEntity>) supply.getInverseCollection(val3)).containsExactlyInAnyOrder(c, d);

        verify(notifier, times(2)).accept(val1);
        verify(notifier, times(0)).accept(val2);
        verify(notifier, times(2)).accept(val3);
        verifyNoMoreInteractions(notifier);

        Mockito.reset(scoreDirector, notifier);

        supply.beforeVariableChanged(scoreDirector, c);
        c.setValue(val2);
        supply.afterVariableChanged(scoreDirector, c);

        assertThat((Collection<TestdataEntity>) supply.getInverseCollection(val1)).containsExactlyInAnyOrder(a, b);
        assertThat((Collection<TestdataEntity>) supply.getInverseCollection(val2)).containsExactly(c);
        assertThat((Collection<TestdataEntity>) supply.getInverseCollection(val3)).containsExactly(d);

        verify(notifier).accept(val3);
        verify(notifier).accept(val2);
        verifyNoMoreInteractions(notifier);

        supply.close();
    }

    @Test
    void cannotExternalizeTwice() {
        var solutionDescriptor = TestdataInverseRelationSolution.buildSolutionDescriptor();
        var entityDescriptor = solutionDescriptor.findEntityDescriptorOrFail(TestdataInverseRelationEntity.class);
        var shadowEntityDescriptor = solutionDescriptor.findEntityDescriptorOrFail(TestdataInverseRelationValue.class);
        var entitiesVariableDescriptor =
                (InverseRelationShadowVariableDescriptor<TestdataInverseRelationSolution>) shadowEntityDescriptor
                        .getShadowVariableDescriptor("entities");
        @SuppressWarnings("unchecked")
        var notifier = (Consumer<Object>) mock(Consumer.class);
        var supply = new ExternalizedBasicVariableStateSupply<>(entityDescriptor.getGenuineVariableDescriptor("value"),
                notifier);
        supply.externalize(entitiesVariableDescriptor);

        assertThatThrownBy(() -> supply.externalize(entitiesVariableDescriptor))
                .isInstanceOf(IllegalStateException.class);
    }

}
