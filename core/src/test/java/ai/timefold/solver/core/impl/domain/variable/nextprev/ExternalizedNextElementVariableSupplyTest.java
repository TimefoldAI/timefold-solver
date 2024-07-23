package ai.timefold.solver.core.impl.domain.variable.nextprev;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListValue;

import org.junit.jupiter.api.Test;

class ExternalizedNextElementVariableSupplyTest {

    @Test
    void updateValues() {
        ListVariableDescriptor<TestdataListSolution> variableDescriptor =
                TestdataListEntity.buildVariableDescriptorForValueList();
        ScoreDirector<TestdataListSolution> scoreDirector = mock(ScoreDirector.class);
        ExternalizedNextElementVariableSupply<TestdataListSolution> supply =
                new ExternalizedNextElementVariableSupply<>(variableDescriptor);

        TestdataListValue v1 = new TestdataListValue("v1");
        TestdataListValue v2 = new TestdataListValue("v2");
        TestdataListValue v3 = new TestdataListValue("v3");
        TestdataListValue v4 = new TestdataListValue("v4");
        TestdataListValue v5 = new TestdataListValue("v5");

        TestdataListEntity e1 = new TestdataListEntity("e1", List.of(v1, v2, v3));
        v1.setEntity(e1);
        v2.setEntity(e1);
        v3.setEntity(e1);
        TestdataListEntity e2 = new TestdataListEntity("e2", List.of(v4, v5));
        v4.setEntity(e2);
        v5.setEntity(e2);

        TestdataListSolution solution = new TestdataListSolution();
        solution.setEntityList(List.of(e1, e2));
        solution.setValueList(List.of(v1, v2, v3, v4, v5));

        when(scoreDirector.getWorkingSolution()).thenReturn(solution);
        supply.resetWorkingSolution(scoreDirector);

        assertThat(supply.getNext(v1)).isSameAs(v2);
        assertThat(supply.getNext(v2)).isSameAs(v3);
        assertThat(supply.getNext(v3)).isNull();

        assertThat(supply.getNext(v4)).isSameAs(v5);
        assertThat(supply.getNext(v5)).isNull();

        // Updating first value
        supply.beforeListVariableChanged(scoreDirector, e1, 0, 1);
        supply.beforeListVariableChanged(scoreDirector, e2, 0, 0);
        v1.setEntity(e2);
        e2.addValueAt(0, v1);
        e1.removeValue(v1);
        supply.afterListVariableChanged(scoreDirector, e1, 0, 0);
        supply.afterListVariableChanged(scoreDirector, e2, 0, 1);

        assertThat(supply.getNext(v2)).isSameAs(v3);
        assertThat(supply.getNext(v3)).isNull();
        assertThat(supply.getNext(v1)).isSameAs(v4);
        assertThat(supply.getNext(v4)).isSameAs(v5);
        assertThat(supply.getNext(v5)).isNull();

        // Updating last value
        supply.beforeListVariableChanged(scoreDirector, e1, 2, 2);
        supply.beforeListVariableChanged(scoreDirector, e2, 0, 1);
        v1.setEntity(e1);
        e1.addValue(v1);
        e2.removeValue(v1);
        supply.afterListVariableChanged(scoreDirector, e1, 2, 3);
        supply.afterListVariableChanged(scoreDirector, e2, 1, 1);

        assertThat(supply.getNext(v2)).isSameAs(v3);
        assertThat(supply.getNext(v3)).isSameAs(v1);
        assertThat(supply.getNext(v1)).isNull();
        assertThat(supply.getNext(v4)).isSameAs(v5);
        assertThat(supply.getNext(v5)).isNull();

        // Updating middle value
        supply.beforeListVariableChanged(scoreDirector, e1, 3, 3);
        supply.beforeListVariableChanged(scoreDirector, e2, 0, 1);
        v4.setEntity(e1);
        e1.addValueAt(1, v4);
        e2.removeValue(v4);
        supply.afterListVariableChanged(scoreDirector, e1, 1, 2);
        supply.afterListVariableChanged(scoreDirector, e2, 1, 1);

        assertThat(supply.getNext(v2)).isSameAs(v4);
        assertThat(supply.getNext(v4)).isSameAs(v3);

        supply.close();
    }

    @Test
    void removeEntity() {
        ListVariableDescriptor<TestdataListSolution> variableDescriptor =
                TestdataListEntity.buildVariableDescriptorForValueList();
        ScoreDirector<TestdataListSolution> scoreDirector = mock(ScoreDirector.class);
        ExternalizedNextElementVariableSupply<TestdataListSolution> supply =
                new ExternalizedNextElementVariableSupply<>(variableDescriptor);

        TestdataListValue v1 = new TestdataListValue("v1");
        TestdataListValue v2 = new TestdataListValue("v2");
        TestdataListValue v3 = new TestdataListValue("v3");
        TestdataListValue v4 = new TestdataListValue("v4");
        TestdataListValue v5 = new TestdataListValue("v5");

        TestdataListEntity e1 = new TestdataListEntity("e1", List.of(v1, v2, v3));
        v1.setEntity(e1);
        v2.setEntity(e1);
        v3.setEntity(e1);
        TestdataListEntity e2 = new TestdataListEntity("e2", List.of(v4, v5));
        v4.setEntity(e2);
        v5.setEntity(e2);

        TestdataListSolution solution = new TestdataListSolution();
        solution.setEntityList(List.of(e1, e2));
        solution.setValueList(List.of(v1, v2, v3, v4, v5));

        when(scoreDirector.getWorkingSolution()).thenReturn(solution);
        supply.resetWorkingSolution(scoreDirector);

        assertThat(supply.getNext(v1)).isSameAs(v2);
        assertThat(supply.getNext(v2)).isSameAs(v3);
        assertThat(supply.getNext(v3)).isNull();

        assertThat(supply.getNext(v4)).isSameAs(v5);
        assertThat(supply.getNext(v5)).isNull();

        // Removing entity
        supply.beforeEntityRemoved(scoreDirector, e1);
        solution.removeEntity(e1);
        supply.afterEntityRemoved(scoreDirector, e1);

        assertThat(supply.getNext(v1)).isNull();
        assertThat(supply.getNext(v2)).isNull();
        assertThat(supply.getNext(v3)).isNull();
        assertThat(supply.getNext(v4)).isSameAs(v5);
        assertThat(supply.getNext(v5)).isNull();

        supply.close();
    }

}
