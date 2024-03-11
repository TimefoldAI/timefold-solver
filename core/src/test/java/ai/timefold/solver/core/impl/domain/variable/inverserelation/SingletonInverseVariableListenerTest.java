package ai.timefold.solver.core.impl.domain.variable.inverserelation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import java.util.Arrays;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ShadowVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.testdata.domain.chained.shadow.TestdataShadowingChainedAnchor;
import ai.timefold.solver.core.impl.testdata.domain.chained.shadow.TestdataShadowingChainedEntity;
import ai.timefold.solver.core.impl.testdata.domain.chained.shadow.TestdataShadowingChainedSolution;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class SingletonInverseVariableListenerTest {

    @Test
    void chainedEntity() {
        SolutionDescriptor<TestdataShadowingChainedSolution> solutionDescriptor =
                TestdataShadowingChainedSolution.buildSolutionDescriptor();
        EntityDescriptor<TestdataShadowingChainedSolution> entityDescriptor =
                solutionDescriptor.findEntityDescriptorOrFail(TestdataShadowingChainedEntity.class);
        ShadowVariableDescriptor<TestdataShadowingChainedSolution> nextEntityVariableDescriptor =
                entityDescriptor.getShadowVariableDescriptor("nextEntity");
        SingletonInverseVariableListener<TestdataShadowingChainedSolution> variableListener =
                new SingletonInverseVariableListener<>(
                        (InverseRelationShadowVariableDescriptor<TestdataShadowingChainedSolution>) nextEntityVariableDescriptor,
                        entityDescriptor.getGenuineVariableDescriptor("chainedObject"));
        InnerScoreDirector<TestdataShadowingChainedSolution, SimpleScore> scoreDirector = mock(InnerScoreDirector.class);

        TestdataShadowingChainedAnchor a0 = new TestdataShadowingChainedAnchor("a0");
        TestdataShadowingChainedEntity a1 = new TestdataShadowingChainedEntity("a1", a0);
        a1.setAnchor(a0);
        a0.setNextEntity(a1);
        TestdataShadowingChainedEntity a2 = new TestdataShadowingChainedEntity("a2", a1);
        a2.setAnchor(a0);
        a1.setNextEntity(a2);
        TestdataShadowingChainedEntity a3 = new TestdataShadowingChainedEntity("a3", a2);
        a3.setAnchor(a0);
        a2.setNextEntity(a3);

        TestdataShadowingChainedAnchor b0 = new TestdataShadowingChainedAnchor("b0");
        TestdataShadowingChainedEntity b1 = new TestdataShadowingChainedEntity("b1", b0);
        b1.setAnchor(b0);
        b0.setNextEntity(b1);

        TestdataShadowingChainedSolution solution = new TestdataShadowingChainedSolution("solution");
        solution.setChainedAnchorList(Arrays.asList(a0, b0));
        solution.setChainedEntityList(Arrays.asList(a1, a2, a3, b1));

        assertThat(b1.getNextEntity()).isEqualTo(null);

        variableListener.beforeVariableChanged(scoreDirector, a3);
        a3.setChainedObject(b1);
        variableListener.afterVariableChanged(scoreDirector, a3);
        assertThat(b1.getNextEntity()).isEqualTo(a3);

        InOrder inOrder = inOrder(scoreDirector);
        inOrder.verify(scoreDirector).beforeVariableChanged(nextEntityVariableDescriptor, b1);
        inOrder.verify(scoreDirector).afterVariableChanged(nextEntityVariableDescriptor, b1);
        inOrder.verifyNoMoreInteractions();
    }

}
