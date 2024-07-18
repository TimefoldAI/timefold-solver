package ai.timefold.solver.core.impl.domain.variable.nextprev;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListValue;

import org.junit.jupiter.api.Test;

class ExternalizedNextElementVariableSupplyTest {

    @Test
    void planningListVariable() {
        ListVariableDescriptor<TestdataListSolution> variableDescriptor =
                TestdataListEntity.buildVariableDescriptorForValueList();
        ScoreDirector<TestdataListSolution> scoreDirector = mock(ScoreDirector.class);
        SingletonInverseVariableSupply inverseVariableSupply = planningValue -> ((TestdataListValue) planningValue).getEntity();
        ExternalizedNextElementVariableSupply<TestdataListSolution> supply =
                new ExternalizedNextElementVariableSupply<>(variableDescriptor, inverseVariableSupply);

        TestdataListValue v1 = new TestdataListValue("v1");
        TestdataListValue v2 = new TestdataListValue("v2");
        TestdataListValue v3 = new TestdataListValue("v3");
        TestdataListValue v4 = new TestdataListValue("v4");
        TestdataListValue v5 = new TestdataListValue("v5");
        TestdataListValue v6 = new TestdataListValue("v6");

        TestdataListEntity e1 = new TestdataListEntity("e1", List.of(v1, v2, v3));
        v1.setEntity(e1);
        v2.setEntity(e1);
        v3.setEntity(e1);
        TestdataListEntity e2 = new TestdataListEntity("e1", List.of(v4, v5));
        v4.setEntity(e2);
        v5.setEntity(e2);

        TestdataListSolution solution = new TestdataListSolution();
        solution.setEntityList(List.of(e1, e2));
        solution.setValueList(List.of(v1, v2, v3, v4, v5, v6));

        when(scoreDirector.getWorkingSolution()).thenReturn(solution);
        supply.resetWorkingSolution(scoreDirector);

        assertThat(supply.getNext(v1)).isSameAs(v2);
        assertThat(supply.getNext(v2)).isSameAs(v3);
        assertThat(supply.getNext(v3)).isNull();

        assertThat(supply.getNext(v4)).isSameAs(v5);
        assertThat(supply.getNext(v5)).isNull();
        assertThat(supply.getNext(v6)).isNull();

        // Updating entity
        supply.beforeVariableChanged(scoreDirector, v5);
        v5.setEntity(e1);
        e1.addValue(v5);
        e2.removeValue(v5);
        supply.afterVariableChanged(scoreDirector, v5);

        assertThat(supply.getNext(v3)).isSameAs(v5);
        assertThat(supply.getNext(v4)).isNull();
        assertThat(supply.getNext(v5)).isNull();

        // Removing entity
        supply.beforeEntityRemoved(scoreDirector, v2);
        e1.removeValue(v2);
        supply.afterEntityRemoved(scoreDirector, v2);

        assertThat(supply.getNext(v1)).isSameAs(v3);
        assertThat(supply.getNext(v3)).isSameAs(v5);

        supply.close();
    }

}
