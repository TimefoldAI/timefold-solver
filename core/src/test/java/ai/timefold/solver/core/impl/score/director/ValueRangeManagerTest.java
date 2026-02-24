package ai.timefold.solver.core.impl.score.director;

import static ai.timefold.solver.core.testutil.PlannerAssert.assertNonNullCodesOfIterator;
import static ai.timefold.solver.core.testutil.PlannerAssert.assertReversedNonNullCodesOfIterator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.Comparator;
import java.util.List;

import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.config.heuristic.selector.common.decorator.SelectionSorterOrder;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.AbstractValueRangeDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.ComparatorFactorySelectionSorter;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.ComparatorSelectionSorter;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorter;
import ai.timefold.solver.core.impl.util.MathUtils;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.clone.lookup.TestdataObjectEquals;
import ai.timefold.solver.core.testdomain.composite.TestdataCompositeEntity;
import ai.timefold.solver.core.testdomain.composite.TestdataCompositeSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.composite.TestdataListCompositeEntity;
import ai.timefold.solver.core.testdomain.list.composite.TestdataListCompositeSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListValue;
import ai.timefold.solver.core.testdomain.list.unassignedvar.composite.TestdataAllowsUnassignedCompositeListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.composite.TestdataAllowsUnassignedCompositeListSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingValue;
import ai.timefold.solver.core.testdomain.list.valuerange.composite.TestdataListCompositeEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.composite.TestdataListCompositeEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.hashcode.TestdataListEntityProvidingHashCodeEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.hashcode.TestdataListEntityProvidingHashCodeSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.unassignedvar.TestdataListUnassignedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.unassignedvar.TestdataListUnassignedEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.unassignedvar.composite.TestdataListUnassignedCompositeEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.unassignedvar.composite.TestdataListUnassignedCompositeEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedEntity;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedSolution;
import ai.timefold.solver.core.testdomain.unassignedvar.composite.TestdataAllowsUnassignedCompositeEntity;
import ai.timefold.solver.core.testdomain.unassignedvar.composite.TestdataAllowsUnassignedCompositeSolution;
import ai.timefold.solver.core.testdomain.valuerange.TestdataValueRangeEntity;
import ai.timefold.solver.core.testdomain.valuerange.TestdataValueRangeSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.TestdataEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.TestdataEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.composite.TestdataCompositeEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.composite.TestdataCompositeEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.hashCode.TestdataEntityProvidingHashCodeEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.hashCode.TestdataEntityProvidingHashCodeSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.composite.TestdataAllowsUnassignedCompositeEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.composite.TestdataAllowsUnassignedCompositeEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.valuerange.hashcode.TestdataValueRangeHashCodeEntity;
import ai.timefold.solver.core.testdomain.valuerange.hashcode.TestdataValueRangeHashCodeSolution;

import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.Test;

class ValueRangeManagerTest {

    @Test
    void extractValueFromSolutionUnassignedBasicVariable() {
        var solution = TestdataAllowsUnassignedSolution.generateSolution(2, 2);
        var valueRangeDescriptor = TestdataAllowsUnassignedEntity.buildVariableDescriptorForValue()
                .getValueRangeDescriptor();
        var solutionDescriptor = valueRangeDescriptor.getVariableDescriptor().getEntityDescriptor().getSolutionDescriptor();
        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);

        // The value range manager will add the null value
        // 2 distinct values
        assertThat(valueRangeManager.countOnSolution(valueRangeDescriptor, solution)).isEqualTo(3);
        var valueRange = (ValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(3);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (ValueRange<?>) valueRangeDescriptor.extractAllValues(solution);
        assertThat(otherValueRange.getSize()).isEqualTo(2);
    }

    @Test
    void sortValueFromSolutionUnassignedBasicVariable() {
        var solution = TestdataAllowsUnassignedSolution.generateSolution(6, 1);
        var valueRangeDescriptor = TestdataAllowsUnassignedEntity.buildVariableDescriptorForValue()
                .getValueRangeDescriptor();
        assertSolutionValueRangeSortingOrder(solution, valueRangeDescriptor, List.of("Generated Value 0", "Generated Value 1",
                "Generated Value 2", "Generated Value 3", "Generated Value 4", "Generated Value 5"));
    }

    @Test
    void extractValueFromSolutionCompositeUnassignedBasicVariable() {
        var solution = TestdataAllowsUnassignedCompositeSolution.generateSolution(2, 2);
        var valueRangeDescriptor = TestdataAllowsUnassignedCompositeEntity.buildVariableDescriptorForValue()
                .getValueRangeDescriptor();
        var solutionDescriptor = valueRangeDescriptor.getVariableDescriptor().getEntityDescriptor().getSolutionDescriptor();
        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);

        // The value range manager will not add the null value
        // valueRange1 [v1, v2] -> 2 distinct values
        // valueRange2 [v3, v4] -> 2 distinct values
        assertThat(valueRangeManager.countOnSolution(valueRangeDescriptor, solution)).isEqualTo(5);
        var valueRange = (ValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(5);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (ValueRange<?>) valueRangeDescriptor.extractAllValues(solution);
        assertThat(otherValueRange.getSize()).isEqualTo(4);
    }

    @Test
    void sortValueFromSolutionCompositeUnassignedBasicVariable() {
        var solution = TestdataAllowsUnassignedCompositeSolution.generateSolution(3, 1);
        var valueRangeDescriptor = TestdataAllowsUnassignedCompositeEntity.buildVariableDescriptorForValue()
                .getValueRangeDescriptor();
        // 3 values per range
        assertSolutionValueRangeSortingOrder(solution, valueRangeDescriptor, List.of("Generated Value 0", "Generated Value 1",
                "Generated Value 2", "Generated Value 3", "Generated Value 4", "Generated Value 5"));
    }

    @Test
    void extractValueFromSolutionAssignedBasicVariable() {
        var solution = TestdataSolution.generateSolution(2, 2);
        var valueRangeDescriptor = TestdataEntity.buildVariableDescriptorForValue()
                .getValueRangeDescriptor();
        var solutionDescriptor = valueRangeDescriptor.getVariableDescriptor().getEntityDescriptor().getSolutionDescriptor();
        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);

        // The value range manager will not add the null value
        // 2 distinct values
        assertThat(valueRangeManager.countOnSolution(valueRangeDescriptor, solution)).isEqualTo(2);
        var valueRange = (ValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(2);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (ValueRange<?>) valueRangeDescriptor.extractAllValues(solution);
        assertThat(otherValueRange.getSize()).isEqualTo(2);
    }

    @Test
    void sortValueFromSolutionAssignedBasicVariable() {
        var solution = TestdataSolution.generateSolution(6, 1);
        var valueRangeDescriptor = TestdataEntity.buildVariableDescriptorForValue().getValueRangeDescriptor();
        assertSolutionValueRangeSortingOrder(solution, valueRangeDescriptor, List.of("Generated Value 0", "Generated Value 1",
                "Generated Value 2", "Generated Value 3", "Generated Value 4", "Generated Value 5"));
    }

    @Test
    void extractValueFromSolutionCompositeAssignedBasicVariable() {
        var solution = TestdataCompositeSolution.generateSolution(2, 2);
        var valueRangeDescriptor = TestdataCompositeSolution.buildSolutionDescriptor()
                .findEntityDescriptor(TestdataCompositeEntity.class)
                .getGenuineVariableDescriptor("value")
                .getValueRangeDescriptor();
        var solutionDescriptor = valueRangeDescriptor.getVariableDescriptor().getEntityDescriptor().getSolutionDescriptor();
        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);

        // The value range manager will not add the null value
        // valueRange1 [v0, v1] -> 2 distinct values
        // valueRange2 [v2, v3] -> 2 distinct values
        assertThat(valueRangeManager.countOnSolution(valueRangeDescriptor, solution)).isEqualTo(4);
        var valueRange = (ValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(4);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (ValueRange<?>) valueRangeDescriptor.extractAllValues(solution);
        assertThat(otherValueRange.getSize()).isEqualTo(4);
    }

    @Test
    void sortValueFromSolutionCompositeAssignedBasicVariable() {
        var solution = TestdataCompositeSolution.generateSolution(3, 1);
        var valueRangeDescriptor = TestdataCompositeSolution.buildSolutionDescriptor()
                .findEntityDescriptor(TestdataCompositeEntity.class)
                .getGenuineVariableDescriptor("value")
                .getValueRangeDescriptor();
        // 3 values per range
        assertSolutionValueRangeSortingOrder(solution, valueRangeDescriptor, List.of("Generated Value 0", "Generated Value 1",
                "Generated Value 2", "Generated Value 3", "Generated Value 4", "Generated Value 5"));
    }

    @Test
    void extractValueFromEntityUnassignedBasicVariable() {
        var solution = TestdataAllowsUnassignedEntityProvidingSolution.generateSolution();
        var valueRangeDescriptor = TestdataAllowsUnassignedEntityProvidingEntity.buildVariableDescriptorForValue()
                .getValueRangeDescriptor();
        var solutionDescriptor = valueRangeDescriptor.getVariableDescriptor().getEntityDescriptor().getSolutionDescriptor();
        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);

        // The value range manager will add the null value
        // Two entities: e1(v1, v2) and e2(v1, v3) -> 3 distinct values
        assertThat(valueRangeManager.countOnSolution(valueRangeDescriptor, solution)).isEqualTo(4);
        var valueRange = (ValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(4);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (ValueRange<?>) valueRangeDescriptor.extractAllValues(solution);
        assertThat(otherValueRange.getSize()).isEqualTo(3);

        // The value range manager will add the null value
        // e1(v1, v2) -> 2 distinct values
        var entity = solution.getEntityList().get(0);
        assertThat(valueRangeManager.countOnEntity(valueRangeDescriptor, entity)).isEqualTo(3);
        var entityValueRange = (ValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, entity);
        assertThat(entityValueRange.getSize()).isEqualTo(3);

        // Fetching from the descriptor does not include the null value
        var otherEntityValueRange = (ValueRange<?>) valueRangeDescriptor.extractValuesFromEntity(solution, entity);
        assertThat(otherEntityValueRange.getSize()).isEqualTo(2);
    }

    @Test
    void sortValueFromEntityUnassignedBasicVariable() {
        var solution = TestdataAllowsUnassignedEntityProvidingSolution.generateUninitializedSolution(6, 2);
        var valueRangeDescriptor = TestdataAllowsUnassignedEntityProvidingEntity.buildVariableDescriptorForValue()
                .getValueRangeDescriptor();
        // 3 values per entity
        assertSolutionValueRangeSortingOrder(solution, valueRangeDescriptor, List.of("Generated Value 0", "Generated Value 1",
                "Generated Value 2", "Generated Value 3", "Generated Value 4", "Generated Value 5"));
        assertEntityValueRangeSortingOrder(solution, solution.getEntityList().get(0), valueRangeDescriptor,
                List.of("Generated Value 0", "Generated Value 1", "Generated Value 2"));
        assertEntityValueRangeSortingOrder(solution, solution.getEntityList().get(1), valueRangeDescriptor,
                List.of("Generated Value 3", "Generated Value 4", "Generated Value 5"));
    }

    @Test
    void extractValueFromEntityCompositeUnassignedBasicVariable() {
        var solution = TestdataAllowsUnassignedCompositeEntityProvidingSolution.generateSolution();
        var valueRangeDescriptor = TestdataAllowsUnassignedCompositeEntityProvidingEntity.buildVariableDescriptorForValue()
                .getValueRangeDescriptor();
        var solutionDescriptor = valueRangeDescriptor.getVariableDescriptor().getEntityDescriptor().getSolutionDescriptor();
        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);

        // The value range manager will add the null value
        // e1([v1, v2], [v1, v4]) -> 3 distinct values
        // e2([v1, v3], [v1, v5]) -> 3 distinct values
        // The composite range returns all distinct values from both ranges -> 6 values
        assertThat(valueRangeManager.countOnSolution(valueRangeDescriptor, solution)).isEqualTo(6);
        var valueRange = (ValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(6);

        // Fetching from the descriptor does include the null value
        var otherValueRange = (ValueRange<?>) valueRangeDescriptor.extractAllValues(solution);
        assertThat(otherValueRange.getSize()).isEqualTo(5);

        // The value range manager will add the null value
        // e1([v1, v2], [v1, v3]) -> 4 distinct values
        var entity = solution.getEntityList().get(0);
        assertThat(valueRangeManager.countOnEntity(valueRangeDescriptor, entity)).isEqualTo(4);
        var entityValueRange = (ValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, entity);
        assertThat(entityValueRange.getSize()).isEqualTo(4);

        // Fetching from the descriptor does include the null value
        var otherEntityValueRange = (ValueRange<?>) valueRangeDescriptor.extractValuesFromEntity(solution, entity);
        assertThat(otherEntityValueRange.getSize()).isEqualTo(3);
    }

    @Test
    void sortValueFromEntityCompositeUnassignedBasicVariable() {
        var solution = TestdataAllowsUnassignedCompositeEntityProvidingSolution.generateSolution(6, 2);
        var valueRangeDescriptor = TestdataAllowsUnassignedCompositeEntityProvidingEntity.buildVariableDescriptorForValue()
                .getValueRangeDescriptor();
        assertSolutionValueRangeSortingOrder(solution, valueRangeDescriptor, List.of("Generated Value 0", "Generated Value 1",
                "Generated Value 2", "Generated Value 3", "Generated Value 4", "Generated Value 5"));
        assertEntityValueRangeSortingOrder(solution, solution.getEntityList().get(0), valueRangeDescriptor,
                List.of("Generated Value 0", "Generated Value 1", "Generated Value 2", "Generated Value 3", "Generated Value 4",
                        "Generated Value 5"));
        assertEntityValueRangeSortingOrder(solution, solution.getEntityList().get(1), valueRangeDescriptor,
                List.of("Generated Value 0", "Generated Value 1", "Generated Value 2", "Generated Value 3", "Generated Value 4",
                        "Generated Value 5"));
    }

    @Test
    void extractValueFromEntityAssignedBasicVariable() {
        var solution = TestdataEntityProvidingSolution.generateSolution();
        var valueRangeDescriptor = TestdataEntityProvidingEntity.buildVariableDescriptorForValue()
                .getValueRangeDescriptor();
        var solutionDescriptor = valueRangeDescriptor.getVariableDescriptor().getEntityDescriptor().getSolutionDescriptor();
        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);

        // The value range manager will add the null value
        // Two entities: e1(v1, v2) and e2(v1, v3) -> 3 distinct values
        assertThat(valueRangeManager.countOnSolution(valueRangeDescriptor, solution)).isEqualTo(3);
        var valueRange = (ValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(3);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (ValueRange<?>) valueRangeDescriptor.extractAllValues(solution);
        assertThat(otherValueRange.getSize()).isEqualTo(3);

        // The value range manager will add the null value
        // e1(v1, v2) -> 2 distinct values
        var entity = solution.getEntityList().get(0);
        assertThat(valueRangeManager.countOnEntity(valueRangeDescriptor, entity)).isEqualTo(2);
        var entityValueRange = (ValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, entity);
        assertThat(entityValueRange.getSize()).isEqualTo(2);

        // Fetching from the descriptor does not include the null value
        var otherEntityValueRange = (ValueRange<?>) valueRangeDescriptor.extractValuesFromEntity(solution, entity);
        assertThat(otherEntityValueRange.getSize()).isEqualTo(2);
    }

    @Test
    void sortValueFromEntityAssignedBasicVariable() {
        var solution = TestdataEntityProvidingSolution.generateUninitializedSolution(6, 2);
        var valueRangeDescriptor = TestdataEntityProvidingEntity.buildVariableDescriptorForValue()
                .getValueRangeDescriptor();
        assertSolutionValueRangeSortingOrder(solution, valueRangeDescriptor, List.of("Generated Value 0", "Generated Value 1",
                "Generated Value 2", "Generated Value 3", "Generated Value 4", "Generated Value 5"));
        assertEntityValueRangeSortingOrder(solution, solution.getEntityList().get(0), valueRangeDescriptor,
                List.of("Generated Value 0", "Generated Value 1", "Generated Value 2"));
        assertEntityValueRangeSortingOrder(solution, solution.getEntityList().get(1), valueRangeDescriptor,
                List.of("Generated Value 3", "Generated Value 4", "Generated Value 5"));
    }

    @Test
    void extractValueFromEntityCompositeAssignedBasicVariable() {
        var solution = TestdataCompositeEntityProvidingSolution.generateSolution();
        var valueRangeDescriptor = TestdataCompositeEntityProvidingEntity.buildVariableDescriptorForValue()
                .getValueRangeDescriptor();
        var solutionDescriptor = valueRangeDescriptor.getVariableDescriptor().getEntityDescriptor().getSolutionDescriptor();
        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);

        // The value range manager will not add the null value
        // e1([v1, v2], [v1, v4]) -> 3 distinct values
        // e2([v1, v3], [v1, v5]) -> 3 distinct values
        // The composite range returns all distinct values from both ranges -> 5 values
        assertThat(valueRangeManager.countOnSolution(valueRangeDescriptor, solution)).isEqualTo(5);
        var valueRange = (ValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(5);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (ValueRange<?>) valueRangeDescriptor.extractAllValues(solution);
        assertThat(otherValueRange.getSize()).isEqualTo(5);

        // The value range manager will not add the null value
        // e1([v1, v2], [v1, v4]) -> 3 distinct values
        // The composite range returns all distinct values from both ranges -> 3 values
        var entity = solution.getEntityList().get(0);
        assertThat(valueRangeManager.countOnEntity(valueRangeDescriptor, entity)).isEqualTo(3);
        var entityValueRange = (ValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, entity);
        assertThat(entityValueRange.getSize()).isEqualTo(3);

        // Fetching from the descriptor does not include the null value
        var otherEntityValueRange = (ValueRange<?>) valueRangeDescriptor.extractValuesFromEntity(solution, entity);
        assertThat(otherEntityValueRange.getSize()).isEqualTo(3);
    }

    @Test
    void sortValueFromEntityCompositeAssignedBasicVariable() {
        var solution = TestdataCompositeEntityProvidingSolution.generateSolution(6, 2);
        var valueRangeDescriptor = TestdataCompositeEntityProvidingEntity.buildVariableDescriptorForValue()
                .getValueRangeDescriptor();
        assertSolutionValueRangeSortingOrder(solution, valueRangeDescriptor, List.of("Generated Value 0", "Generated Value 1",
                "Generated Value 2", "Generated Value 3", "Generated Value 4", "Generated Value 5"));
        assertEntityValueRangeSortingOrder(solution, solution.getEntityList().get(0), valueRangeDescriptor,
                List.of("Generated Value 0", "Generated Value 1", "Generated Value 2", "Generated Value 3", "Generated Value 4",
                        "Generated Value 5"));
        assertEntityValueRangeSortingOrder(solution, solution.getEntityList().get(1), valueRangeDescriptor,
                List.of("Generated Value 0", "Generated Value 1", "Generated Value 2", "Generated Value 3", "Generated Value 4",
                        "Generated Value 5"));
    }

    @Test
    void extractValueFromSolutionUnassignedListVariable() {
        var solution = TestdataAllowsUnassignedValuesListSolution.generateUninitializedSolution(2, 2);
        var valueRangeDescriptor = TestdataAllowsUnassignedValuesListEntity.buildVariableDescriptorForValueList()
                .getValueRangeDescriptor();
        var solutionDescriptor = valueRangeDescriptor.getVariableDescriptor().getEntityDescriptor().getSolutionDescriptor();
        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);

        // The value range manager will not add the null value because it is a list variable
        // 2 distinct values
        assertThat(valueRangeManager.countOnSolution(valueRangeDescriptor, solution)).isEqualTo(2);
        var valueRange = (ValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(2);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (ValueRange<?>) valueRangeDescriptor.extractAllValues(solution);
        assertThat(otherValueRange.getSize()).isEqualTo(2);
    }

    @Test
    void sortValueFromSolutionUnassignedListVariable() {
        var solution = TestdataAllowsUnassignedValuesListSolution.generateUninitializedSolution(6, 2);
        var valueRangeDescriptor = TestdataAllowsUnassignedValuesListEntity.buildVariableDescriptorForValueList()
                .getValueRangeDescriptor();
        assertSolutionValueRangeSortingOrder(solution, valueRangeDescriptor, List.of("Generated Value 0", "Generated Value 1",
                "Generated Value 2", "Generated Value 3", "Generated Value 4", "Generated Value 5"));
    }

    @Test
    void extractValueFromSolutionCompositeUnassignedListVariable() {
        var solution = TestdataAllowsUnassignedCompositeListSolution.generateSolution(2, 2);
        var valueRangeDescriptor = TestdataAllowsUnassignedCompositeListEntity.buildVariableDescriptorForValueList()
                .getValueRangeDescriptor();
        var solutionDescriptor = TestdataAllowsUnassignedCompositeListSolution.buildSolutionDescriptor();
        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);

        // The value range manager will not add the null value because it is a list variable
        // valueRange1 [v1, v2] -> 2 distinct values
        // valueRange2 [v3, v4] -> 2 distinct values
        assertThat(valueRangeManager.countOnSolution(valueRangeDescriptor, solution)).isEqualTo(4);
        var valueRange = (ValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(4);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (ValueRange<?>) valueRangeDescriptor.extractAllValues(solution);
        assertThat(otherValueRange.getSize()).isEqualTo(4);
    }

    @Test
    void sortValueFromSolutionCompositeUnassignedListVariable() {
        var solution = TestdataAllowsUnassignedCompositeListSolution.generateSolution(3, 2);
        var valueRangeDescriptor = TestdataAllowsUnassignedCompositeListEntity.buildVariableDescriptorForValueList()
                .getValueRangeDescriptor();
        // 3 values per range
        assertSolutionValueRangeSortingOrder(solution, valueRangeDescriptor, List.of("Generated Value 0", "Generated Value 1",
                "Generated Value 2", "Generated Value 3", "Generated Value 4", "Generated Value 5"));
    }

    @Test
    void extractValueFromSolutionAssignedListVariable() {
        var solution = TestdataListSolution.generateUninitializedSolution(2, 2);
        var valueRangeDescriptor = TestdataListSolution.buildSolutionDescriptor()
                .findEntityDescriptor(TestdataListEntity.class)
                .getGenuineVariableDescriptor("valueList")
                .getValueRangeDescriptor();
        var solutionDescriptor = valueRangeDescriptor.getVariableDescriptor().getEntityDescriptor().getSolutionDescriptor();
        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);

        // The value range manager will not add the null value
        // 2 distinct values
        assertThat(valueRangeManager.countOnSolution(valueRangeDescriptor, solution)).isEqualTo(2);
        var valueRange = (ValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(2);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (ValueRange<?>) valueRangeDescriptor.extractAllValues(solution);
        assertThat(otherValueRange.getSize()).isEqualTo(2);
    }

    @Test
    void sortValueFromSolutionAssignedListVariable() {
        var solution = TestdataListSolution.generateUninitializedSolution(6, 2);
        var valueRangeDescriptor = TestdataListSolution.buildSolutionDescriptor()
                .findEntityDescriptor(TestdataListEntity.class)
                .getGenuineVariableDescriptor("valueList")
                .getValueRangeDescriptor();
        assertSolutionValueRangeSortingOrder(solution, valueRangeDescriptor, List.of("Generated Value 0", "Generated Value 1",
                "Generated Value 2", "Generated Value 3", "Generated Value 4", "Generated Value 5"));
    }

    @Test
    void extractValueFromSolutionCompositeAssignedListVariable() {
        var solution = TestdataListCompositeSolution.generateSolution(2, 2);
        var valueRangeDescriptor = TestdataListCompositeEntity.buildVariableDescriptorForValueList()
                .getValueRangeDescriptor();
        var solutionDescriptor = valueRangeDescriptor.getVariableDescriptor().getEntityDescriptor().getSolutionDescriptor();
        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);

        // The value range manager will not add the null value
        // valueRange1 [v0, v1] -> 2 distinct values
        // valueRange2 [v2, v3] -> 2 distinct values
        assertThat(valueRangeManager.countOnSolution(valueRangeDescriptor, solution)).isEqualTo(4);
        var valueRange = (ValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(4);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (ValueRange<?>) valueRangeDescriptor.extractAllValues(solution);
        assertThat(otherValueRange.getSize()).isEqualTo(4);
    }

    @Test
    void sortValueFromSolutionCompositeAssignedListVariable() {
        var solution = TestdataListCompositeSolution.generateSolution(3, 2);
        var valueRangeDescriptor = TestdataListCompositeEntity.buildVariableDescriptorForValueList()
                .getValueRangeDescriptor();
        // 3 values per range
        assertSolutionValueRangeSortingOrder(solution, valueRangeDescriptor, List.of("Generated Value 0", "Generated Value 1",
                "Generated Value 2", "Generated Value 3", "Generated Value 4", "Generated Value 5"));
    }

    @Test
    void extractValueFromEntityUnassignedListVariable() {
        var solution = TestdataListUnassignedEntityProvidingSolution.generateSolution();
        var valueRangeDescriptor = TestdataListUnassignedEntityProvidingEntity.buildVariableDescriptorForValueList()
                .getValueRangeDescriptor();
        var solutionDescriptor = valueRangeDescriptor.getVariableDescriptor().getEntityDescriptor().getSolutionDescriptor();
        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);

        // The value range manager will not add the null value because it is a list variable
        // Two entities: e1(v1, v2) and e2(v1, v3) -> 3 distinct values
        assertThat(valueRangeManager.countOnSolution(valueRangeDescriptor, solution)).isEqualTo(3);
        var valueRange = (ValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(3);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (ValueRange<?>) valueRangeDescriptor.extractAllValues(solution);
        assertThat(otherValueRange.getSize()).isEqualTo(3);

        // The value range manager will not add the null value because it is a list variable
        // e1(v1, v2) -> 2 distinct values
        var entity = solution.getEntityList().get(0);
        assertThat(valueRangeManager.countOnEntity(valueRangeDescriptor, entity)).isEqualTo(2);
        var entityValueRange = (ValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, entity);
        assertThat(entityValueRange.getSize()).isEqualTo(2);

        // Fetching from the descriptor does not include the null value
        var otherEntityValueRange = (ValueRange<?>) valueRangeDescriptor.extractValuesFromEntity(solution, entity);
        assertThat(otherEntityValueRange.getSize()).isEqualTo(2);
    }

    @Test
    void sortValueFromEntityUnassignedListVariable() {
        var solution = TestdataListUnassignedEntityProvidingSolution.generateSolution(6, 2);
        var valueRangeDescriptor = TestdataListUnassignedEntityProvidingEntity.buildVariableDescriptorForValueList()
                .getValueRangeDescriptor();
        assertSolutionValueRangeSortingOrder(solution, valueRangeDescriptor, List.of("Generated Value 0", "Generated Value 1",
                "Generated Value 2", "Generated Value 3", "Generated Value 4", "Generated Value 5"));
        assertEntityValueRangeSortingOrder(solution, solution.getEntityList().get(0), valueRangeDescriptor,
                List.of("Generated Value 0", "Generated Value 1", "Generated Value 2"));
        assertEntityValueRangeSortingOrder(solution, solution.getEntityList().get(1), valueRangeDescriptor,
                List.of("Generated Value 3", "Generated Value 4", "Generated Value 5"));
    }

    @Test
    void extractValueFromEntityCompositeUnassignedListVariable() {
        var solution = TestdataListUnassignedCompositeEntityProvidingSolution.generateSolution();
        var valueRangeDescriptor = TestdataListUnassignedCompositeEntityProvidingEntity.buildVariableDescriptorForValueList()
                .getValueRangeDescriptor();
        var solutionDescriptor = valueRangeDescriptor.getVariableDescriptor().getEntityDescriptor().getSolutionDescriptor();
        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);

        // The value range manager will not add the null value because it is a list variable
        // e1([v1, v2], [v1, v3]) -> 3 distinct values
        // e2([v1, v4], [v1, v5]) -> 3 distinct values
        // The composite range returns all distinct values -> 5 values
        assertThat(valueRangeManager.countOnSolution(valueRangeDescriptor, solution)).isEqualTo(5);
        var valueRange = (ValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(5);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (ValueRange<?>) valueRangeDescriptor.extractAllValues(solution);
        assertThat(otherValueRange.getSize()).isEqualTo(5);

        // The value range manager will not add the null value because it is a list variable
        // e1([v1, v2], [v1, v3]) -> 3 distinct values
        // The composite range returns all distinct values from both ranges -> 3 values
        var entity = solution.getEntityList().get(0);
        assertThat(valueRangeManager.countOnEntity(valueRangeDescriptor, entity)).isEqualTo(3);
        var entityValueRange = (ValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, entity);
        assertThat(entityValueRange.getSize()).isEqualTo(3);

        // Fetching from the descriptor does not include the null value
        var otherEntityValueRange = (ValueRange<?>) valueRangeDescriptor.extractValuesFromEntity(solution, entity);
        assertThat(otherEntityValueRange.getSize()).isEqualTo(3);
    }

    @Test
    void sortValueFromEntityCompositeUnassignedListVariable() {
        var solution = TestdataListUnassignedCompositeEntityProvidingSolution.generateSolution(6, 2);
        var valueRangeDescriptor = TestdataListUnassignedCompositeEntityProvidingEntity.buildVariableDescriptorForValueList()
                .getValueRangeDescriptor();
        assertSolutionValueRangeSortingOrder(solution, valueRangeDescriptor, List.of("Generated Value 0", "Generated Value 1",
                "Generated Value 2", "Generated Value 3", "Generated Value 4", "Generated Value 5"));
        assertEntityValueRangeSortingOrder(solution, solution.getEntityList().get(0), valueRangeDescriptor,
                List.of("Generated Value 0", "Generated Value 1", "Generated Value 2", "Generated Value 3", "Generated Value 4",
                        "Generated Value 5"));
        assertEntityValueRangeSortingOrder(solution, solution.getEntityList().get(1), valueRangeDescriptor,
                List.of("Generated Value 0", "Generated Value 1", "Generated Value 2", "Generated Value 3", "Generated Value 4",
                        "Generated Value 5"));
    }

    @Test
    void extractValueFromEntityAssignedListVariable() {
        var solution = TestdataListEntityProvidingSolution.generateSolution();
        var valueRangeDescriptor = TestdataListEntityProvidingEntity.buildVariableDescriptorForValueList()
                .getValueRangeDescriptor();
        var solutionDescriptor = valueRangeDescriptor.getVariableDescriptor().getEntityDescriptor().getSolutionDescriptor();
        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);

        // The value range manager will not add the null value because it is a list variable
        // Two entities: e1(v1, v2) and e2(v1, v3) -> 3 distinct values
        assertThat(valueRangeManager.countOnSolution(valueRangeDescriptor, solution)).isEqualTo(3);
        var valueRange = (ValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(3);

        // Fetching from the descriptor does not include the null value because it is a list variable
        var otherValueRange = (ValueRange<?>) valueRangeDescriptor.extractAllValues(solution);
        assertThat(otherValueRange.getSize()).isEqualTo(3);

        // The value range manager will not add the null value because it is a list variable
        // e1(v1, v2) -> 2 distinct values
        var entity = solution.getEntityList().get(0);
        assertThat(valueRangeManager.countOnEntity(valueRangeDescriptor, entity)).isEqualTo(2);
        var entityValueRange = (ValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, entity);
        assertThat(entityValueRange.getSize()).isEqualTo(2);

        // Fetching from the descriptor does not include the null value
        var otherEntityValueRange = (ValueRange<?>) valueRangeDescriptor.extractValuesFromEntity(solution, entity);
        assertThat(otherEntityValueRange.getSize()).isEqualTo(2);
    }

    @Test
    void sortValueFromEntityAssignedListVariable() {
        var solution = TestdataListEntityProvidingSolution.generateSolution(6, 2, false);
        var valueRangeDescriptor = TestdataListEntityProvidingEntity.buildVariableDescriptorForValueList()
                .getValueRangeDescriptor();
        assertSolutionValueRangeSortingOrder(solution, valueRangeDescriptor, List.of("Generated Value 0", "Generated Value 1",
                "Generated Value 2", "Generated Value 3", "Generated Value 4", "Generated Value 5"));
        assertEntityValueRangeSortingOrder(solution, solution.getEntityList().get(0), valueRangeDescriptor,
                List.of("Generated Value 0", "Generated Value 1", "Generated Value 2"));
        assertEntityValueRangeSortingOrder(solution, solution.getEntityList().get(1), valueRangeDescriptor,
                List.of("Generated Value 3", "Generated Value 4", "Generated Value 5"));
    }

    @Test
    void extractValueFromEntityCompositeAssignedListVariable() {
        var solution = TestdataListCompositeEntityProvidingSolution.generateSolution();
        var valueRangeDescriptor = TestdataListCompositeEntityProvidingEntity.buildVariableDescriptorForValueList()
                .getValueRangeDescriptor();
        var solutionDescriptor = valueRangeDescriptor.getVariableDescriptor().getEntityDescriptor().getSolutionDescriptor();
        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);

        // The value range manager will not add the null value because it is a list variable
        // e1([v1, v2], [v1, v3]) -> 3 distinct values
        // e2([v1, v4], [v1, v5]) -> 3 distinct values
        // The composite range returns all distinct values from both ranges -> 5 values
        assertThat(valueRangeManager.countOnSolution(valueRangeDescriptor, solution)).isEqualTo(5);
        var valueRange = (ValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(5);

        // Fetching from the descriptor does not include the null value because it is a list variable
        var otherValueRange = (ValueRange<?>) valueRangeDescriptor.extractAllValues(solution);
        assertThat(otherValueRange.getSize()).isEqualTo(5);

        // The value range manager will add the null value because it is a list variable
        // e1([v1, v2], [v1, v3]) -> 3 distinct values
        // The composite range returns all distinct values from both ranges -> 3 values
        var entity = solution.getEntityList().get(0);
        assertThat(valueRangeManager.countOnEntity(valueRangeDescriptor, entity)).isEqualTo(3);
        var entityValueRange = (ValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, entity);
        assertThat(entityValueRange.getSize()).isEqualTo(3);

        // Fetching from the descriptor does not include the null value
        var otherEntityValueRange = (ValueRange<?>) valueRangeDescriptor.extractValuesFromEntity(solution, entity);
        assertThat(otherEntityValueRange.getSize()).isEqualTo(3);
    }

    @Test
    void sortValueFromEntityCompositeAssignedListVariable() {
        var solution = TestdataListCompositeEntityProvidingSolution.generateSolution(6, 2);
        var valueRangeDescriptor = TestdataListCompositeEntityProvidingEntity.buildVariableDescriptorForValueList()
                .getValueRangeDescriptor();
        assertSolutionValueRangeSortingOrder(solution, valueRangeDescriptor, List.of("Generated Value 0", "Generated Value 1",
                "Generated Value 2", "Generated Value 3", "Generated Value 4", "Generated Value 5"));
        assertEntityValueRangeSortingOrder(solution, solution.getEntityList().get(0), valueRangeDescriptor,
                List.of("Generated Value 0", "Generated Value 1", "Generated Value 2", "Generated Value 3", "Generated Value 4",
                        "Generated Value 5"));
        assertEntityValueRangeSortingOrder(solution, solution.getEntityList().get(1), valueRangeDescriptor,
                List.of("Generated Value 0", "Generated Value 1", "Generated Value 2", "Generated Value 3", "Generated Value 4",
                        "Generated Value 5"));
    }

    @Test
    void countEntities() {
        var valueCount = 10;
        var entityCount = 3;
        var solution = TestdataListSolution.generateInitializedSolution(valueCount, entityCount);
        var solutionDescriptor = TestdataListSolution.buildSolutionDescriptor();
        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);

        var initializationStats = valueRangeManager.getInitializationStatistics();
        assertThat(initializationStats.genuineEntityCount()).isEqualTo(entityCount);
        assertThat(initializationStats.shadowEntityCount()).isEqualTo(valueCount);
    }

    @Test
    void countUninitializedVariables() {
        var valueCount = 10;
        var entityCount = 3;
        var solution = TestdataSolution.generateSolution(valueCount, entityCount);
        var solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);

        var initializationStats = valueRangeManager.getInitializationStatistics();
        assertThat(initializationStats.uninitializedVariableCount()).isZero();

        solution.getEntityList().get(0).setValue(null);
        valueRangeManager.reset(solution);

        initializationStats = valueRangeManager.getInitializationStatistics();
        assertThat(initializationStats.uninitializedVariableCount()).isOne();

        solution.getEntityList().forEach(entity -> entity.setValue(null));
        valueRangeManager.reset(solution);

        initializationStats = valueRangeManager.getInitializationStatistics();
        assertThat(initializationStats.uninitializedVariableCount()).isEqualTo(entityCount);
    }

    @Test
    void countUnassignedValues() {
        var valueCount = 10;
        var entityCount = 3;
        var solution = TestdataListSolution.generateInitializedSolution(valueCount, entityCount);
        var solutionDescriptor = TestdataListSolution.buildSolutionDescriptor();
        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);

        var initializationStats = valueRangeManager.getInitializationStatistics();
        assertThat(initializationStats.unassignedValueCount()).isZero();

        var valueList = solution.getEntityList().get(0).getValueList();
        var unassignedValueCount = valueList.size();
        assertThat(valueList).hasSizeGreaterThan(10 / 3);
        valueList.forEach(value -> {
            value.setEntity(null);
            value.setIndex(null);
        });
        valueList.clear();
        valueRangeManager.reset(solution);

        initializationStats = valueRangeManager.getInitializationStatistics();
        assertThat(initializationStats.unassignedValueCount()).isEqualTo(unassignedValueCount);
    }

    @Test
    void problemScaleBasic() {
        var valueCount = 10;
        var entityCount = 20;
        var solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        var solution = TestdataSolution.generateSolution(valueCount, entityCount);

        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);
        assertSoftly(softly -> {
            softly.assertThat(solutionDescriptor.getGenuineEntityCount(solution)).isEqualTo(entityCount);
            softly.assertThat(solutionDescriptor.getGenuineVariableCount(solution)).isEqualTo(entityCount);
            softly.assertThat(valueRangeManager.getStatistics().getMaximumValueRangeSize()).isEqualTo(valueCount);
            softly.assertThat(valueRangeManager.getStatistics().getApproximateValueCount()).isEqualTo(valueCount);
            softly.assertThat(valueRangeManager.getStatistics().getProblemScale())
                    .isEqualTo(20.0);
        });
    }

    @Test
    void emptyProblemScale() {
        var valueCount = 27;
        var entityCount = 27;
        var solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        var solution = TestdataSolution.generateSolution(valueCount, entityCount);
        solution.getValueList().clear();

        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);
        assertSoftly(softly -> {
            softly.assertThat(solutionDescriptor.getGenuineEntityCount(solution)).isEqualTo(entityCount);
            softly.assertThat(solutionDescriptor.getGenuineVariableCount(solution)).isEqualTo(entityCount);
            softly.assertThat(valueRangeManager.getStatistics().getMaximumValueRangeSize()).isEqualTo(0);
            softly.assertThat(valueRangeManager.getStatistics().getApproximateValueCount()).isEqualTo(0);
            softly.assertThat(valueRangeManager.getStatistics().getProblemScale())
                    .isEqualTo(0);
        });
    }

    @Test
    void emptyProblemScaleAllowsUnassigned() {
        var valueCount = 27;
        var entityCount = 27;
        var solutionDescriptor = TestdataAllowsUnassignedSolution.buildSolutionDescriptor();
        var solution = TestdataAllowsUnassignedSolution.generateSolution(valueCount, entityCount);
        solution.getValueList().clear();

        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);
        assertSoftly(softly -> {
            softly.assertThat(solutionDescriptor.getGenuineEntityCount(solution)).isEqualTo(entityCount);
            softly.assertThat(solutionDescriptor.getGenuineVariableCount(solution)).isEqualTo(entityCount);
            softly.assertThat(valueRangeManager.getStatistics().getMaximumValueRangeSize()).isEqualTo(1);
            softly.assertThat(valueRangeManager.getStatistics().getApproximateValueCount()).isEqualTo(1);
            softly.assertThat(valueRangeManager.getStatistics().getProblemScale())
                    .isEqualTo(0);
        });
    }

    @Test
    void problemScaleMultipleValueRanges() {
        var solutionDescriptor = TestdataValueRangeSolution.buildSolutionDescriptor();
        var solution = new TestdataValueRangeSolution("Solution");
        solution.setEntityList(List.of(new TestdataValueRangeEntity("A")));

        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);
        final var entityCount = 1L;
        final var valueCount = 3L;
        final var variableCount = 8L;
        assertSoftly(softly -> {
            softly.assertThat(solutionDescriptor.getGenuineEntityCount(solution)).isEqualTo(entityCount);
            softly.assertThat(solutionDescriptor.getGenuineVariableCount(solution)).isEqualTo(entityCount * variableCount);
            softly.assertThat(valueRangeManager.getStatistics().getMaximumValueRangeSize()).isEqualTo(3L);
            softly.assertThat(valueRangeManager.getStatistics().getApproximateValueCount())
                    .isEqualTo(variableCount * valueCount);
            softly.assertThat(valueRangeManager.getStatistics().getProblemScale())
                    .isCloseTo(Math.log10(Math.pow(valueCount, variableCount)), Percentage.withPercentage(1.0));
        });
    }

    @Test
    void basicVariableProblemScaleEntityProvidingValueRange() {
        var solutionDescriptor = TestdataAllowsUnassignedEntityProvidingSolution.buildSolutionDescriptor();
        var solution = new TestdataAllowsUnassignedEntityProvidingSolution("Solution");
        var v1 = new TestdataValue("1");
        var v2 = new TestdataValue("2");
        solution.setEntityList(List.of(
                new TestdataAllowsUnassignedEntityProvidingEntity("A",
                        List.of(v1, v2)),
                new TestdataAllowsUnassignedEntityProvidingEntity("B",
                        List.of(v1, v2, new TestdataValue("3")))));

        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);
        assertSoftly(softly -> {
            softly.assertThat(solutionDescriptor.getGenuineEntityCount(solution)).isEqualTo(2L);
            softly.assertThat(solutionDescriptor.getGenuineVariableCount(solution)).isEqualTo(2L);

            // Add 1 to the value range sizes, since the value range allows unassigned
            softly.assertThat(valueRangeManager.getStatistics().getMaximumValueRangeSize()).isEqualTo(4L);
            softly.assertThat(valueRangeManager.getStatistics().getApproximateValueCount()).isEqualTo(3L + 4L);
            softly.assertThat(valueRangeManager.getStatistics().getProblemScale())
                    .isCloseTo(Math.log10(3 * 4), Percentage.withPercentage(1.0));
        });
    }

    @Test
    void listVariableProblemScaleEntityProvidingValueRange() {
        var solutionDescriptor = TestdataListEntityProvidingSolution.buildSolutionDescriptor();
        var solution = new TestdataListEntityProvidingSolution();
        var v1 = new TestdataListEntityProvidingValue("1");
        var v2 = new TestdataListEntityProvidingValue("2");
        solution.setEntityList(List.of(
                new TestdataListEntityProvidingEntity("e1", List.of(v1, v2)),
                new TestdataListEntityProvidingEntity("e2", List.of(v1, v2, new TestdataListEntityProvidingValue("3")))));

        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);
        assertSoftly(softly -> {
            softly.assertThat(solutionDescriptor.getGenuineEntityCount(solution)).isEqualTo(2L);
            softly.assertThat(solutionDescriptor.getGenuineVariableCount(solution)).isEqualTo(2L);

            softly.assertThat(valueRangeManager.getStatistics().getMaximumValueRangeSize()).isEqualTo(3L);
            softly.assertThat(valueRangeManager.getStatistics().getApproximateValueCount()).isEqualTo(2L + 3L);
        });
    }

    @Test
    void problemScaleSingleEntityProvidingSingleValueRange() {
        var solutionDescriptor = TestdataAllowsUnassignedEntityProvidingSolution.buildSolutionDescriptor();
        var solution = new TestdataAllowsUnassignedEntityProvidingSolution("Solution");

        var v1 = new TestdataValue("1");
        solution.setEntityList(List.of(
                new TestdataAllowsUnassignedEntityProvidingEntity("A",
                        List.of(v1))));

        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);
        assertSoftly(softly -> {
            softly.assertThat(solutionDescriptor.getGenuineEntityCount(solution)).isEqualTo(1L);
            softly.assertThat(solutionDescriptor.getGenuineVariableCount(solution)).isEqualTo(1L);

            // Add 1 to the value range sizes, since the value range allows unassigned
            softly.assertThat(valueRangeManager.getStatistics().getMaximumValueRangeSize()).isEqualTo(2L);
            softly.assertThat(valueRangeManager.getStatistics().getApproximateValueCount()).isEqualTo(2L);
            softly.assertThat(valueRangeManager.getStatistics().getProblemScale())
                    .isCloseTo(Math.log10(2), Percentage.withPercentage(1.0));
        });
    }

    @Test
    void problemScaleList() {
        var valueCount = 500;
        var entityCount = 20;
        var solutionDescriptor = TestdataListSolution.buildSolutionDescriptor();
        var solution = TestdataListSolution.generateUninitializedSolution(valueCount, entityCount);

        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);
        assertSoftly(softly -> {
            softly.assertThat(solutionDescriptor.getGenuineEntityCount(solution)).isEqualTo(entityCount);
            softly.assertThat(solutionDescriptor.getGenuineVariableCount(solution)).isEqualTo(entityCount);
            softly.assertThat(valueRangeManager.getStatistics().getMaximumValueRangeSize()).isEqualTo(valueCount);
            softly.assertThat(valueRangeManager.getStatistics().getApproximateValueCount()).isEqualTo(valueCount);
            softly.assertThat(valueRangeManager.getStatistics().getProblemScale())
                    .isCloseTo(MathUtils.getPossibleArrangementsScaledApproximateLog(MathUtils.LOG_PRECISION, 10, 500, 20)
                            / (double) MathUtils.LOG_PRECISION, Percentage.withPercentage(1.0));
        });
    }

    @Test
    void problemScaleSingleEntityWithAssignedValues() {
        var valueCount = 1;
        var entityCount = 1;
        var solutionDescriptor = TestdataListSolution.buildSolutionDescriptor();
        var solution = TestdataListSolution.generateUninitializedSolution(valueCount, entityCount);

        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);
        assertSoftly(softly -> {
            softly.assertThat(solutionDescriptor.getGenuineEntityCount(solution)).isEqualTo(entityCount);
            softly.assertThat(solutionDescriptor.getGenuineVariableCount(solution)).isEqualTo(entityCount);
            softly.assertThat(valueRangeManager.getStatistics().getMaximumValueRangeSize()).isEqualTo(valueCount);
            softly.assertThat(valueRangeManager.getStatistics().getApproximateValueCount()).isEqualTo(valueCount);
            softly.assertThat(valueRangeManager.getStatistics().getProblemScale()).isEqualTo(0.0);
        });
    }

    @Test
    void problemScaleSingleEntityWithUnassignedValues() {
        var valueCount = 1;
        var entityCount = 1;
        var solutionDescriptor = TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor();
        var solution = TestdataAllowsUnassignedValuesListSolution.generateUninitializedSolution(valueCount, entityCount);

        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);
        assertSoftly(softly -> {
            softly.assertThat(solutionDescriptor.getGenuineEntityCount(solution)).isEqualTo(entityCount);
            softly.assertThat(solutionDescriptor.getGenuineVariableCount(solution)).isEqualTo(entityCount);
            softly.assertThat(valueRangeManager.getStatistics().getMaximumValueRangeSize()).isEqualTo(valueCount);
            softly.assertThat(valueRangeManager.getStatistics().getApproximateValueCount()).isEqualTo(valueCount);
            softly.assertThat(valueRangeManager.getStatistics().getProblemScale())
                    .isCloseTo(Math.log10(2), Percentage.withPercentage(1.0));
        });
    }

    @Test
    void deduplicationForSolution() {
        var valueCount = 3;
        var entityCount = 3;
        var solutionDescriptor = TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor();
        var valueRangeDescriptor =
                TestdataAllowsUnassignedValuesListEntity.buildVariableDescriptorForValueList().getValueRangeDescriptor();
        var solution = TestdataAllowsUnassignedValuesListSolution.generateUninitializedSolution(valueCount, entityCount);

        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);

        var valueRange =
                (ValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, solution.getEntityList().get(0));
        assertNonNullCodesOfIterator(valueRange.createOriginalIterator(),
                solution.getValueList().stream().map(TestdataAllowsUnassignedValuesListValue::getCode).toArray(String[]::new));
        var otherValueRange =
                (ValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, solution.getEntityList().get(1));
        assertThat(valueRange).isSameAs(otherValueRange);
        var yetAnotherValueRange =
                (ValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, solution.getEntityList().get(2));
        assertThat(yetAnotherValueRange).isSameAs(otherValueRange);
    }

    @Test
    void deduplicationForEntity() {
        var valueCount = 3;
        var entityCount = 3;
        var solutionDescriptor = TestdataListEntityProvidingSolution.buildSolutionDescriptor();
        var valueRangeDescriptor =
                TestdataListEntityProvidingEntity.buildVariableDescriptorForValueList().getValueRangeDescriptor();
        var solution = TestdataListEntityProvidingSolution.generateSolution(valueCount, entityCount, true);

        // One entity with a different range
        solution.getEntityList().get(2).getValueRange().remove(0);

        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);

        // Entity 0 and Entity 1
        var valueRange =
                (ValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, solution.getEntityList().get(0));
        assertNonNullCodesOfIterator(valueRange.createOriginalIterator(),
                solution.getValueList().stream().map(TestdataListEntityProvidingValue::getCode).toArray(String[]::new));
        var otherValueRange =
                (ValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, solution.getEntityList().get(1));
        assertThat(valueRange).isSameAs(otherValueRange);

        // Entity 2
        var yetAnotherValueRange =
                (ValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, solution.getEntityList().get(2));
        assertThat(yetAnotherValueRange).isNotSameAs(otherValueRange);

        // Sorting data
        SelectionSorter<TestdataListEntityProvidingSolution, TestdataObject> sorterComparator =
                new ComparatorSelectionSorter<>(Comparator.comparing(TestdataObject::getCode), SelectionSorterOrder.DESCENDING);
        valueRange = valueRangeManager.getFromEntity(valueRangeDescriptor, solution.getEntityList().get(0), sorterComparator);
        otherValueRange =
                valueRangeManager.getFromEntity(valueRangeDescriptor, solution.getEntityList().get(1), sorterComparator);
        assertThat(valueRange).isSameAs(otherValueRange);
        yetAnotherValueRange =
                valueRangeManager.getFromEntity(valueRangeDescriptor, solution.getEntityList().get(2), sorterComparator);
        assertThat(yetAnotherValueRange).isNotSameAs(otherValueRange);
    }

    @Test
    void failExtractAllValuesFromEntityRangeForBasicVariable() {
        var solution = new TestdataEntityProvidingHashCodeSolution("s1");
        var entity1 = new TestdataEntityProvidingHashCodeEntity("e1",
                List.of(new TestdataObjectEquals(1), new TestdataObjectEquals(2)));
        var entity2 = new TestdataEntityProvidingHashCodeEntity("e2",
                List.of(new TestdataObjectEquals(3), new TestdataObjectEquals(1)));
        // We have two values 1 with different instances, which is not allowed
        solution.setEntityList(List.of(entity1, entity2));

        var valueRangeDescriptor = TestdataEntityProvidingHashCodeEntity.buildVariableDescriptorForValue()
                .getValueRangeDescriptor();
        var solutionDescriptor = valueRangeDescriptor.getVariableDescriptor().getEntityDescriptor().getSolutionDescriptor();
        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);

        assertThatThrownBy(() -> valueRangeManager.countOnSolution(valueRangeDescriptor, solution))
                .hasMessageContaining("The value range")
                .hasMessageContaining("already includes the value")
                .hasMessageContaining("but a different instance with the same identity")
                .hasMessageContaining("was found")
                .hasMessageContaining(
                        "Values that are considered identical according to equals/hashCode semantics must not have different instances in value ranges");
    }

    @Test
    void failExtractAllValuesFromEntityRangeForListVariable() {
        var solution = new TestdataListEntityProvidingHashCodeSolution();
        var entity1 = new TestdataListEntityProvidingHashCodeEntity("e1",
                List.of(new TestdataObjectEquals(1), new TestdataObjectEquals(2)));
        var entity2 = new TestdataListEntityProvidingHashCodeEntity("e2",
                List.of(new TestdataObjectEquals(3), new TestdataObjectEquals(1)));
        // We have two values 1 with different instances, which is not allowed
        solution.setEntityList(List.of(entity1, entity2));

        var valueRangeDescriptor = TestdataListEntityProvidingHashCodeEntity.buildVariableDescriptorForValueList()
                .getValueRangeDescriptor();
        var solutionDescriptor = valueRangeDescriptor.getVariableDescriptor().getEntityDescriptor().getSolutionDescriptor();
        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);

        assertThatThrownBy(() -> valueRangeManager.countOnSolution(valueRangeDescriptor, solution))
                .hasMessageContaining("The value range")
                .hasMessageContaining("already includes the value")
                .hasMessageContaining("but a different instance with the same identity")
                .hasMessageContaining("was found")
                .hasMessageContaining(
                        "Values that are considered identical according to equals/hashCode semantics must not have different instances in value ranges");
    }

    @Test
    void failExtractAllValuesFromSolutionRangeForListVariable() {
        var solution = new TestdataValueRangeHashCodeSolution();
        var entity1 = new TestdataValueRangeHashCodeEntity("e1");
        var entity2 = new TestdataValueRangeHashCodeEntity("e2");
        solution.setEntityList(List.of(entity1, entity2));
        // We have two values 1 with different instances, which is not allowed
        solution.setValueList(List.of(new TestdataObjectEquals(1), new TestdataObjectEquals(1)));

        var valueRangeDescriptor = TestdataValueRangeHashCodeEntity.buildVariableDescriptorForValue()
                .getValueRangeDescriptor();
        var solutionDescriptor = valueRangeDescriptor.getVariableDescriptor().getEntityDescriptor().getSolutionDescriptor();
        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);

        assertThatThrownBy(() -> valueRangeManager.countOnSolution(valueRangeDescriptor, solution))
                .hasMessageContaining("The value range")
                .hasMessageContaining("already includes the value")
                .hasMessageContaining("but a different instance with the same identity")
                .hasMessageContaining("was found")
                .hasMessageContaining(
                        "Values that are considered identical according to equals/hashCode semantics must not have different instances in value ranges");
    }

    private <Solution_> void assertSolutionValueRangeSortingOrder(Solution_ solution,
            AbstractValueRangeDescriptor<Solution_> valueRangeDescriptor, List<String> allValues) {
        var solutionDescriptor = valueRangeDescriptor.getVariableDescriptor().getEntityDescriptor().getSolutionDescriptor();
        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);

        // Default order
        var valueRange = (ValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertNonNullCodesOfIterator(valueRange.createOriginalIterator(), allValues.toArray(String[]::new));

        // Desc comparator
        SelectionSorter<Solution_, TestdataObject> sorterComparator =
                new ComparatorSelectionSorter<>(Comparator.comparing(TestdataObject::getCode), SelectionSorterOrder.DESCENDING);
        var sortedValueRange =
                (ValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution, sorterComparator);
        assertReversedNonNullCodesOfIterator(sortedValueRange.createOriginalIterator(), allValues.toArray(String[]::new));
        assertThat(valueRange).isNotSameAs(sortedValueRange);

        // Asc comparator
        // Default order is still desc
        var otherValueRange = (ValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertReversedNonNullCodesOfIterator(otherValueRange.createOriginalIterator(), allValues.toArray(String[]::new));
        assertThat(otherValueRange).isSameAs(sortedValueRange);

        // Add the asc sorter
        SelectionSorter<Solution_, TestdataObject> sorterComparatorFactory =
                new ComparatorFactorySelectionSorter<>(sol -> Comparator.comparing(TestdataObject::getCode),
                        SelectionSorterOrder.ASCENDING);
        var otherSortedValueRange =
                (ValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution,
                        sorterComparatorFactory);
        assertNonNullCodesOfIterator(otherSortedValueRange.createOriginalIterator(), allValues.toArray(String[]::new));
        assertThat(otherSortedValueRange).isNotSameAs(otherValueRange);

        // Using the same sorter
        var anotherSortedValueRange =
                (ValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution,
                        sorterComparatorFactory);
        assertThat(otherSortedValueRange).isSameAs(anotherSortedValueRange);
    }

    private <Solution_, Entity_> void assertEntityValueRangeSortingOrder(Solution_ solution, Entity_ entity,
            AbstractValueRangeDescriptor<Solution_> valueRangeDescriptor, List<String> allValues) {
        var solutionDescriptor = valueRangeDescriptor.getVariableDescriptor().getEntityDescriptor().getSolutionDescriptor();
        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);

        // Default order
        var valueRange = (ValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, entity);
        assertNonNullCodesOfIterator(valueRange.createOriginalIterator(), allValues.toArray(String[]::new));

        // Desc comparator
        SelectionSorter<Solution_, TestdataObject> sorterComparator =
                new ComparatorSelectionSorter<>(Comparator.comparing(TestdataObject::getCode), SelectionSorterOrder.DESCENDING);
        var sortedValueRange =
                (ValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, entity, sorterComparator);
        assertReversedNonNullCodesOfIterator(sortedValueRange.createOriginalIterator(), allValues.toArray(String[]::new));
        assertThat(valueRange).isNotSameAs(sortedValueRange);

        // Asc comparator
        // Default order is still desc
        var otherValueRange = (ValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, entity);
        assertReversedNonNullCodesOfIterator(otherValueRange.createOriginalIterator(), allValues.toArray(String[]::new));
        assertThat(otherValueRange).isSameAs(sortedValueRange);

        // Add the asc sorter
        SelectionSorter<Solution_, TestdataObject> sorterComparatorFactory =
                new ComparatorFactorySelectionSorter<>(sol -> Comparator.comparing(TestdataObject::getCode),
                        SelectionSorterOrder.ASCENDING);
        var otherSortedValueRange =
                (ValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, entity,
                        sorterComparatorFactory);
        assertNonNullCodesOfIterator(otherSortedValueRange.createOriginalIterator(), allValues.toArray(String[]::new));
        assertThat(otherSortedValueRange).isNotSameAs(otherValueRange);

        // Using the same sorter
        var anotherSortedValueRange =
                (ValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, entity,
                        sorterComparatorFactory);
        assertThat(otherSortedValueRange).isSameAs(anotherSortedValueRange);
    }

}
