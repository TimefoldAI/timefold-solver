package ai.timefold.solver.core.impl.domain.variable.inverserelation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

import org.junit.jupiter.api.Test;

class ExternalizedCollectionInverseVariableSupplyTest {

    @Test
    void normal() {
        GenuineVariableDescriptor<TestdataSolution> variableDescriptor = TestdataEntity.buildVariableDescriptorForValue();
        ScoreDirector<TestdataSolution> scoreDirector = mock(ScoreDirector.class);
        ExternalizedCollectionInverseVariableSupply<TestdataSolution> supply =
                new ExternalizedCollectionInverseVariableSupply<>(variableDescriptor);

        TestdataValue val1 = new TestdataValue("1");
        TestdataValue val2 = new TestdataValue("2");
        TestdataValue val3 = new TestdataValue("3");
        TestdataEntity a = new TestdataEntity("a", val1);
        TestdataEntity b = new TestdataEntity("b", val1);
        TestdataEntity c = new TestdataEntity("c", val3);
        TestdataEntity d = new TestdataEntity("d", val3);

        TestdataSolution solution = new TestdataSolution("solution");
        solution.setEntityList(Arrays.asList(a, b, c, d));
        solution.setValueList(Arrays.asList(val1, val2, val3));

        when(scoreDirector.getWorkingSolution()).thenReturn(solution);
        supply.resetWorkingSolution(scoreDirector);

        assertThat((Collection<TestdataEntity>) supply.getInverseCollection(val1)).containsExactlyInAnyOrder(a, b);
        assertThat((Collection<TestdataEntity>) supply.getInverseCollection(val2)).isEmpty();
        assertThat((Collection<TestdataEntity>) supply.getInverseCollection(val3)).containsExactlyInAnyOrder(c, d);

        supply.beforeVariableChanged(scoreDirector, c);
        c.setValue(val2);
        supply.afterVariableChanged(scoreDirector, c);

        assertThat((Collection<TestdataEntity>) supply.getInverseCollection(val1)).containsExactlyInAnyOrder(a, b);
        assertThat((Collection<TestdataEntity>) supply.getInverseCollection(val2)).containsExactly(c);
        assertThat((Collection<TestdataEntity>) supply.getInverseCollection(val3)).containsExactly(d);

        supply.close();
    }

}
