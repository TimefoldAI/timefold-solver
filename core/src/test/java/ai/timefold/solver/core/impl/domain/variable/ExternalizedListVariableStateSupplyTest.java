package ai.timefold.solver.core.impl.domain.variable;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListValue;

import org.junit.jupiter.api.Test;

class ExternalizedListVariableStateSupplyTest {

    @Test
    void initializeRoundTrip() {
        var variableDescriptor = TestdataAllowsUnassignedValuesListEntity.buildVariableDescriptorForValueList();
        var scoreDirector = (ScoreDirector<TestdataAllowsUnassignedValuesListSolution>) mock(InnerScoreDirector.class);
        try (var supply = new ExternalizedListVariableStateSupply<>(variableDescriptor)) {

            var v1 = new TestdataAllowsUnassignedValuesListValue("1");
            var v2 = new TestdataAllowsUnassignedValuesListValue("2");
            var v3 = new TestdataAllowsUnassignedValuesListValue("3");
            var e1 = new TestdataAllowsUnassignedValuesListEntity("e1", v1);
            var e2 = new TestdataAllowsUnassignedValuesListEntity("e2");

            var solution = new TestdataAllowsUnassignedValuesListSolution();
            solution.setEntityList(new ArrayList<>(Arrays.asList(e1, e2)));
            solution.setValueList(Arrays.asList(v1, v2, v3));

            when(scoreDirector.getWorkingSolution()).thenReturn(solution);
            supply.resetWorkingSolution(scoreDirector);

            assertSoftly(softly -> {
                softly.assertThat(supply.getUnassignedCount()).isEqualTo(2);
                softly.assertThat(supply.isAssigned(v1)).isTrue();
                softly.assertThat(supply.isAssigned(v2)).isFalse();
                softly.assertThat(supply.isAssigned(v3)).isFalse();
            });
        }
    }

    @Test
    void assignRoundTrip() {
        var variableDescriptor = TestdataAllowsUnassignedValuesListEntity.buildVariableDescriptorForValueList();
        var scoreDirector = (ScoreDirector<TestdataAllowsUnassignedValuesListSolution>) mock(InnerScoreDirector.class);
        try (var supply = new ExternalizedListVariableStateSupply<>(variableDescriptor)) {

            var v1 = new TestdataAllowsUnassignedValuesListValue("1");
            var v2 = new TestdataAllowsUnassignedValuesListValue("2");
            var v3 = new TestdataAllowsUnassignedValuesListValue("3");
            var e1 = new TestdataAllowsUnassignedValuesListEntity("e1", v1);
            var e2 = new TestdataAllowsUnassignedValuesListEntity("e2");

            var solution = new TestdataAllowsUnassignedValuesListSolution();
            solution.setEntityList(new ArrayList<>(Arrays.asList(e1, e2)));
            solution.setValueList(Arrays.asList(v1, v2, v3));

            when(scoreDirector.getWorkingSolution()).thenReturn(solution);
            supply.resetWorkingSolution(scoreDirector);

            assertSoftly(softly -> {
                softly.assertThat(supply.getUnassignedCount()).isEqualTo(2);
                softly.assertThat(supply.getElementPosition(v1)).isEqualTo(ElementPosition.of(e1, 0));
                softly.assertThat(supply.getElementPosition(v2)).isEqualTo(ElementPosition.unassigned());
                softly.assertThat(supply.getElementPosition(v3)).isEqualTo(ElementPosition.unassigned());
            });

            supply.afterListVariableElementUnassigned(scoreDirector, v1);
            assertSoftly(softly -> {
                softly.assertThat(supply.getUnassignedCount()).isEqualTo(3);
                softly.assertThat(supply.getElementPosition(v1)).isEqualTo(ElementPosition.unassigned());
                softly.assertThat(supply.getElementPosition(v2)).isEqualTo(ElementPosition.unassigned());
                softly.assertThat(supply.getElementPosition(v3)).isEqualTo(ElementPosition.unassigned());
            });

            // Cannot unassign again.
            assertThatThrownBy(() -> supply.afterListVariableElementUnassigned(scoreDirector, v1))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

}
