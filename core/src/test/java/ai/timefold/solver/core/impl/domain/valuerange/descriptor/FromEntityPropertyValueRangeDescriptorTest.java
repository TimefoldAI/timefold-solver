package ai.timefold.solver.core.impl.domain.valuerange.descriptor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingValue;

import org.junit.jupiter.api.Test;

class FromEntityPropertyValueRangeDescriptorTest {

    // ************************************************************************
    // Helper methods
    // ************************************************************************

    private static TestdataListEntityProvidingSolution solutionOf(TestdataListEntityProvidingEntity... entities) {
        var solution = new TestdataListEntityProvidingSolution();
        solution.setEntityList(List.of(entities));
        return solution;
    }

    private static ValueRange<TestdataListEntityProvidingValue>
            extractAllValues(TestdataListEntityProvidingSolution solution) {
        var valueRangeDescriptor =
                TestdataListEntityProvidingEntity.buildVariableDescriptorForValueList().getValueRangeDescriptor();
        return valueRangeDescriptor.extractAllValues(solution);
    }

    private static List<TestdataListEntityProvidingValue> toList(ValueRange<TestdataListEntityProvidingValue> valueRange) {
        var valueList = new ArrayList<TestdataListEntityProvidingValue>();
        for (var i = 0L; i < valueRange.getSize(); i++) {
            valueList.add(valueRange.get(i));
        }
        return valueList;
    }

    // ************************************************************************
    // Extract all values
    // ************************************************************************

    @Test
    void extractAllValuesWithSharedValueRangeInstance() {
        var v1 = new TestdataListEntityProvidingValue("v1");
        var v2 = new TestdataListEntityProvidingValue("v2");
        var v3 = new TestdataListEntityProvidingValue("v3");
        var v4 = new TestdataListEntityProvidingValue("v4");
        var sharedValueList = List.of(v1, v2, v3);
        var solution = solutionOf(
                new TestdataListEntityProvidingEntity("e1", sharedValueList),
                new TestdataListEntityProvidingEntity("e2", sharedValueList),
                new TestdataListEntityProvidingEntity("e3", List.of(v3, v4)));

        var valueRange = extractAllValues(solution);

        assertThat(valueRange.getSize()).isEqualTo(4L);
        assertThat(toList(valueRange)).containsExactly(v1, v2, v3, v4);
    }

    @Test
    void extractAllValuesWithSharedValueRangeInstanceAfterAnotherRange() {
        var v1 = new TestdataListEntityProvidingValue("v1");
        var v2 = new TestdataListEntityProvidingValue("v2");
        var v3 = new TestdataListEntityProvidingValue("v3");
        var v4 = new TestdataListEntityProvidingValue("v4");
        var sharedValueList = List.of(v1, v2, v3);
        var solution = solutionOf(
                new TestdataListEntityProvidingEntity("e1", List.of(v3, v4)),
                new TestdataListEntityProvidingEntity("e2", sharedValueList),
                new TestdataListEntityProvidingEntity("e3", sharedValueList));

        var valueRange = extractAllValues(solution);

        assertThat(valueRange.getSize()).isEqualTo(4L);
        assertThat(toList(valueRange)).containsExactly(v3, v4, v1, v2);
    }

    @Test
    void extractAllValuesWithEqualValueRangesInDifferentInstances() {
        var v1 = new TestdataListEntityProvidingValue("v1");
        var v2 = new TestdataListEntityProvidingValue("v2");
        var solution = solutionOf(
                new TestdataListEntityProvidingEntity("e1", new ArrayList<>(List.of(v1, v2))),
                new TestdataListEntityProvidingEntity("e2", new ArrayList<>(List.of(v1, v2))));

        var valueRange = extractAllValues(solution);

        assertThat(valueRange.getSize()).isEqualTo(2L);
        assertThat(toList(valueRange)).containsExactly(v1, v2);
    }

    @Test
    void extractAllValuesWithAllEntitiesSharingOneValueRangeInstance() {
        var v1 = new TestdataListEntityProvidingValue("v1");
        var v2 = new TestdataListEntityProvidingValue("v2");
        var sharedValueList = List.of(v1, v2);
        var solution = solutionOf(
                new TestdataListEntityProvidingEntity("e1", sharedValueList),
                new TestdataListEntityProvidingEntity("e2", sharedValueList),
                new TestdataListEntityProvidingEntity("e3", sharedValueList));

        var valueRange = extractAllValues(solution);

        assertThat(valueRange.getSize()).isEqualTo(2L);
        assertThat(toList(valueRange)).containsExactly(v1, v2);
        assertThat(valueRange.contains(v1)).isTrue();
        assertThat(valueRange.contains(new TestdataListEntityProvidingValue("v3"))).isFalse();
    }

}
