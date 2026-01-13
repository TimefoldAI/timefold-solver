package ai.timefold.solver.core.impl.domain.variable.inverserelation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.function.Consumer;

import ai.timefold.solver.core.impl.domain.variable.BasicVariableChangeEvent;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.testdomain.chained.TestdataChainedAnchor;
import ai.timefold.solver.core.testdomain.chained.TestdataChainedEntity;
import ai.timefold.solver.core.testdomain.chained.TestdataChainedSolution;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ExternalizedSingletonInverseVariableSupplyTest {

    @Test
    void chainedEntity() {
        GenuineVariableDescriptor<TestdataChainedSolution> variableDescriptor =
                TestdataChainedEntity.buildVariableDescriptorForChainedObject();
        var scoreDirector = mock(InnerScoreDirector.class);
        @SuppressWarnings("unchecked")
        var notifier = (Consumer<Object>) mock(Consumer.class);
        ExternalizedSingletonInverseVariableSupply<TestdataChainedSolution> supply =
                new ExternalizedSingletonInverseVariableSupply<>(variableDescriptor, notifier);

        TestdataChainedAnchor a0 = new TestdataChainedAnchor("a0");
        TestdataChainedEntity a1 = new TestdataChainedEntity("a1", a0);
        TestdataChainedEntity a2 = new TestdataChainedEntity("a2", a1);
        TestdataChainedEntity a3 = new TestdataChainedEntity("a3", a2);

        TestdataChainedAnchor b0 = new TestdataChainedAnchor("b0");
        TestdataChainedEntity b1 = new TestdataChainedEntity("b1", b0);

        TestdataChainedSolution solution = new TestdataChainedSolution("solution");
        solution.setChainedAnchorList(Arrays.asList(a0, b0));
        solution.setChainedEntityList(Arrays.asList(a1, a2, a3, b1));

        when(scoreDirector.getWorkingSolution()).thenReturn(solution);
        supply.resetWorkingSolution(scoreDirector);

        assertThat(supply.getInverseSingleton(a0)).isSameAs(a1);
        assertThat(supply.getInverseSingleton(a1)).isSameAs(a2);
        assertThat(supply.getInverseSingleton(a2)).isSameAs(a3);
        assertThat(supply.getInverseSingleton(a3)).isSameAs(null);
        assertThat(supply.getInverseSingleton(b0)).isSameAs(b1);
        assertThat(supply.getInverseSingleton(b1)).isSameAs(null);

        verify(notifier).accept(a0);
        verify(notifier).accept(a1);
        verify(notifier).accept(a2);
        verify(notifier).accept(b0);
        // b1 and a3 are not updated as they have no inverse
        verifyNoMoreInteractions(notifier);

        Mockito.reset(notifier);

        supply.beforeChange(scoreDirector, new BasicVariableChangeEvent<>(a3));
        a3.setChainedObject(b1);
        supply.afterChange(scoreDirector, new BasicVariableChangeEvent<>(a3));

        assertThat(supply.getInverseSingleton(a2)).isSameAs(null);
        assertThat(supply.getInverseSingleton(b1)).isSameAs(a3);

        verify(notifier).accept(a2);
        verify(notifier).accept(b1);
        verifyNoMoreInteractions(notifier);

        supply.close();
    }

}
