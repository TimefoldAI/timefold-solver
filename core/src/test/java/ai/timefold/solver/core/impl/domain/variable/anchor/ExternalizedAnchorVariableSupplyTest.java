package ai.timefold.solver.core.impl.domain.variable.anchor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import ai.timefold.solver.core.impl.domain.variable.BasicVariableChangeEvent;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.ExternalizedSingletonInverseVariableSupply;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.testdomain.chained.TestdataChainedAnchor;
import ai.timefold.solver.core.testdomain.chained.TestdataChainedEntity;
import ai.timefold.solver.core.testdomain.chained.TestdataChainedSolution;

import org.junit.jupiter.api.Test;

class ExternalizedAnchorVariableSupplyTest {

    @Test
    void chainedEntity() {
        var variableDescriptor = TestdataChainedEntity.buildVariableDescriptorForChainedObject();
        var scoreDirector = mock(InnerScoreDirector.class);
        var nextVariableSupply = new ExternalizedSingletonInverseVariableSupply<>(variableDescriptor);
        var supply = new ExternalizedAnchorVariableSupply<>(variableDescriptor, nextVariableSupply);

        var a0 = new TestdataChainedAnchor("a0");
        var a1 = new TestdataChainedEntity("a1", a0);
        var a2 = new TestdataChainedEntity("a2", a1);
        var a3 = new TestdataChainedEntity("a3", a2);

        var b0 = new TestdataChainedAnchor("b0");
        var b1 = new TestdataChainedEntity("b1", b0);

        var solution = new TestdataChainedSolution("solution");
        solution.setChainedAnchorList(Arrays.asList(a0, b0));
        solution.setChainedEntityList(Arrays.asList(a1, a2, a3, b1));

        when(scoreDirector.getWorkingSolution()).thenReturn(solution);
        nextVariableSupply.resetWorkingSolution(scoreDirector);
        supply.resetWorkingSolution(scoreDirector);

        assertThat(supply.getAnchor(a1)).isSameAs(a0);
        assertThat(supply.getAnchor(a2)).isSameAs(a0);
        assertThat(supply.getAnchor(a3)).isSameAs(a0);
        assertThat(supply.getAnchor(b1)).isSameAs(b0);

        var event = new BasicVariableChangeEvent<Object>(a3);
        nextVariableSupply.beforeChange(scoreDirector, event);
        supply.beforeChange(scoreDirector, event);
        a3.setChainedObject(b1);
        nextVariableSupply.afterChange(scoreDirector, event);
        supply.afterChange(scoreDirector, event);

        assertThat(supply.getAnchor(a1)).isSameAs(a0);
        assertThat(supply.getAnchor(a2)).isSameAs(a0);
        assertThat(supply.getAnchor(a3)).isSameAs(b0);
        assertThat(supply.getAnchor(b1)).isSameAs(b0);

        nextVariableSupply.close();
        supply.close();
    }

}
