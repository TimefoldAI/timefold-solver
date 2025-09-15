package ai.timefold.solver.core.impl.domain.variable.inverserelation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Arrays;

import ai.timefold.solver.core.impl.domain.variable.BasicVariableChangeEvent;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.testdomain.shadow.inverserelation.TestdataInverseRelationEntity;
import ai.timefold.solver.core.testdomain.shadow.inverserelation.TestdataInverseRelationSolution;
import ai.timefold.solver.core.testdomain.shadow.inverserelation.TestdataInverseRelationValue;

import org.junit.jupiter.api.Test;

class CollectionInverseVariableListenerTest {

    @Test
    void normal() {
        var scoreDirector = mock(InnerScoreDirector.class);
        var solutionDescriptor = TestdataInverseRelationSolution.buildSolutionDescriptor();
        var entityDescriptor = solutionDescriptor.findEntityDescriptorOrFail(TestdataInverseRelationEntity.class);
        var shadowEntityDescriptor = solutionDescriptor.findEntityDescriptorOrFail(TestdataInverseRelationValue.class);
        var entitiesVariableDescriptor = shadowEntityDescriptor.getShadowVariableDescriptor("entities");
        var variableListener =
                new CollectionInverseVariableListener<>(
                        (InverseRelationShadowVariableDescriptor<TestdataInverseRelationSolution>) entitiesVariableDescriptor,
                        entityDescriptor.getGenuineVariableDescriptor("value"));

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

        variableListener.beforeChange(scoreDirector, new BasicVariableChangeEvent<>(c));
        c.setValue(val2);
        variableListener.afterChange(scoreDirector, new BasicVariableChangeEvent<>(c));

        assertThat(val1.getEntities()).containsExactly(a, b);
        assertThat(val2.getEntities()).containsExactly(c);
        assertThat(val3.getEntities()).containsExactly(d);
    }

}
