package ai.timefold.solver.core.impl.domain.variable.inverserelation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;

import ai.timefold.solver.core.impl.domain.variable.BasicVariableChangeEvent;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.junit.jupiter.api.Test;

class ExternalizedCollectionInverseVariableSupplyTest {

    @Test
    void normal() {
        var variableDescriptor = TestdataEntity.buildVariableDescriptorForValue();
        var scoreDirector = mock(InnerScoreDirector.class);
        var supply = new ExternalizedCollectionInverseVariableSupply<>(variableDescriptor);

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

        supply.beforeChange(scoreDirector, new BasicVariableChangeEvent<>(c));
        c.setValue(val2);
        supply.afterChange(scoreDirector, new BasicVariableChangeEvent<>(c));

        assertThat((Collection<TestdataEntity>) supply.getInverseCollection(val1)).containsExactlyInAnyOrder(a, b);
        assertThat((Collection<TestdataEntity>) supply.getInverseCollection(val2)).containsExactly(c);
        assertThat((Collection<TestdataEntity>) supply.getInverseCollection(val3)).containsExactly(d);

        supply.close();
    }

}
