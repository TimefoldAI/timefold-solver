package ai.timefold.solver.core.impl.score.director;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.impl.util.MathUtils;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.chained.TestdataChainedAnchor;
import ai.timefold.solver.core.testdomain.chained.TestdataChainedEntity;
import ai.timefold.solver.core.testdomain.chained.TestdataChainedSolution;
import ai.timefold.solver.core.testdomain.composite.TestdataCompositeEntity;
import ai.timefold.solver.core.testdomain.composite.TestdataCompositeSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.composite.TestdataListCompositeEntity;
import ai.timefold.solver.core.testdomain.list.composite.TestdataListCompositeSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.composite.TestdataAllowsUnassignedCompositeListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.composite.TestdataAllowsUnassignedCompositeListSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingValue;
import ai.timefold.solver.core.testdomain.list.valuerange.composite.TestdataListCompositeEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.composite.TestdataListCompositeEntityProvidingSolution;
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
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.composite.TestdataAllowsUnassignedCompositeEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.composite.TestdataAllowsUnassignedCompositeEntityProvidingSolution;

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
        var valueRange = (CountableValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(3);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractAllValues(solution);
        assertThat(otherValueRange.getSize()).isEqualTo(2);
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
        var valueRange = (CountableValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(5);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractAllValues(solution);
        assertThat(otherValueRange.getSize()).isEqualTo(4);
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
        var valueRange = (CountableValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(2);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractAllValues(solution);
        assertThat(otherValueRange.getSize()).isEqualTo(2);
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
        var valueRange = (CountableValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(4);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractAllValues(solution);
        assertThat(otherValueRange.getSize()).isEqualTo(4);
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
        var valueRange = (CountableValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(4);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractAllValues(solution);
        assertThat(otherValueRange.getSize()).isEqualTo(3);

        // The value range manager will add the null value
        // e1(v1, v2) -> 2 distinct values
        var entity = solution.getEntityList().get(0);
        assertThat(valueRangeManager.countOnEntity(valueRangeDescriptor, entity)).isEqualTo(3);
        var entityValueRange = (CountableValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, entity);
        assertThat(entityValueRange.getSize()).isEqualTo(3);

        // Fetching from the descriptor does not include the null value
        var otherEntityValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractValuesFromEntity(solution, entity);
        assertThat(otherEntityValueRange.getSize()).isEqualTo(2);
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
        var valueRange = (CountableValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(6);

        // Fetching from the descriptor does include the null value
        var otherValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractAllValues(solution);
        assertThat(otherValueRange.getSize()).isEqualTo(5);

        // The value range manager will add the null value
        // e1([v1, v2], [v1, v3]) -> 4 distinct values
        var entity = solution.getEntityList().get(0);
        assertThat(valueRangeManager.countOnEntity(valueRangeDescriptor, entity)).isEqualTo(4);
        var entityValueRange = (CountableValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, entity);
        assertThat(entityValueRange.getSize()).isEqualTo(4);

        // Fetching from the descriptor does include the null value
        var otherEntityValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractValuesFromEntity(solution, entity);
        assertThat(otherEntityValueRange.getSize()).isEqualTo(3);
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
        var valueRange = (CountableValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(3);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractAllValues(solution);
        assertThat(otherValueRange.getSize()).isEqualTo(3);

        // The value range manager will add the null value
        // e1(v1, v2) -> 2 distinct values
        var entity = solution.getEntityList().get(0);
        assertThat(valueRangeManager.countOnEntity(valueRangeDescriptor, entity)).isEqualTo(2);
        var entityValueRange = (CountableValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, entity);
        assertThat(entityValueRange.getSize()).isEqualTo(2);

        // Fetching from the descriptor does not include the null value
        var otherEntityValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractValuesFromEntity(solution, entity);
        assertThat(otherEntityValueRange.getSize()).isEqualTo(2);
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
        var valueRange = (CountableValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(5);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractAllValues(solution);
        assertThat(otherValueRange.getSize()).isEqualTo(5);

        // The value range manager will not add the null value
        // e1([v1, v2], [v1, v4]) -> 3 distinct values
        // The composite range returns all distinct values from both ranges -> 3 values
        var entity = solution.getEntityList().get(0);
        assertThat(valueRangeManager.countOnEntity(valueRangeDescriptor, entity)).isEqualTo(3);
        var entityValueRange = (CountableValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, entity);
        assertThat(entityValueRange.getSize()).isEqualTo(3);

        // Fetching from the descriptor does not include the null value
        var otherEntityValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractValuesFromEntity(solution, entity);
        assertThat(otherEntityValueRange.getSize()).isEqualTo(3);
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
        var valueRange = (CountableValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(2);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractAllValues(solution);
        assertThat(otherValueRange.getSize()).isEqualTo(2);
    }

    @Test
    void extractValueFromSolutionCompositeUnassignedListVariable() {
        var solution = TestdataAllowsUnassignedCompositeListSolution.generateSolution(2, 2);
        var valueRangeDescriptor = TestdataAllowsUnassignedCompositeListEntity.buildVariableDescriptorForValueList()
                .getValueRangeDescriptor();
        var solutionDescriptor = valueRangeDescriptor.getVariableDescriptor().getEntityDescriptor().getSolutionDescriptor();
        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);

        // The value range manager will not add the null value because it is a list variable
        // valueRange1 [v1, v2] -> 2 distinct values
        // valueRange2 [v3, v4] -> 2 distinct values
        assertThat(valueRangeManager.countOnSolution(valueRangeDescriptor, solution)).isEqualTo(4);
        var valueRange = (CountableValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(4);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractAllValues(solution);
        assertThat(otherValueRange.getSize()).isEqualTo(4);
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
        var valueRange = (CountableValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(2);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractAllValues(solution);
        assertThat(otherValueRange.getSize()).isEqualTo(2);
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
        var valueRange = (CountableValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(4);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractAllValues(solution);
        assertThat(otherValueRange.getSize()).isEqualTo(4);
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
        var valueRange = (CountableValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(3);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractAllValues(solution);
        assertThat(otherValueRange.getSize()).isEqualTo(3);

        // The value range manager will not add the null value because it is a list variable
        // e1(v1, v2) -> 2 distinct values
        var entity = solution.getEntityList().get(0);
        assertThat(valueRangeManager.countOnEntity(valueRangeDescriptor, entity)).isEqualTo(2);
        var entityValueRange = (CountableValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, entity);
        assertThat(entityValueRange.getSize()).isEqualTo(2);

        // Fetching from the descriptor does not include the null value
        var otherEntityValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractValuesFromEntity(solution, entity);
        assertThat(otherEntityValueRange.getSize()).isEqualTo(2);
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
        var valueRange = (CountableValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(5);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractAllValues(solution);
        assertThat(otherValueRange.getSize()).isEqualTo(5);

        // The value range manager will not add the null value because it is a list variable
        // e1([v1, v2], [v1, v3]) -> 3 distinct values
        // The composite range returns all distinct values from both ranges -> 3 values
        var entity = solution.getEntityList().get(0);
        assertThat(valueRangeManager.countOnEntity(valueRangeDescriptor, entity)).isEqualTo(3);
        var entityValueRange = (CountableValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, entity);
        assertThat(entityValueRange.getSize()).isEqualTo(3);

        // Fetching from the descriptor does not include the null value
        var otherEntityValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractValuesFromEntity(solution, entity);
        assertThat(otherEntityValueRange.getSize()).isEqualTo(3);
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
        var valueRange = (CountableValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(3);

        // Fetching from the descriptor does not include the null value because it is a list variable
        var otherValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractAllValues(solution);
        assertThat(otherValueRange.getSize()).isEqualTo(3);

        // The value range manager will not add the null value because it is a list variable
        // e1(v1, v2) -> 2 distinct values
        var entity = solution.getEntityList().get(0);
        assertThat(valueRangeManager.countOnEntity(valueRangeDescriptor, entity)).isEqualTo(2);
        var entityValueRange = (CountableValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, entity);
        assertThat(entityValueRange.getSize()).isEqualTo(2);

        // Fetching from the descriptor does not include the null value
        var otherEntityValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractValuesFromEntity(solution, entity);
        assertThat(otherEntityValueRange.getSize()).isEqualTo(2);
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
        var valueRange = (CountableValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(5);

        // Fetching from the descriptor does not include the null value because it is a list variable
        var otherValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractAllValues(solution);
        assertThat(otherValueRange.getSize()).isEqualTo(5);

        // The value range manager will add the null value because it is a list variable
        // e1([v1, v2], [v1, v3]) -> 3 distinct values
        // The composite range returns all distinct values from both ranges -> 3 values
        var entity = solution.getEntityList().get(0);
        assertThat(valueRangeManager.countOnEntity(valueRangeDescriptor, entity)).isEqualTo(3);
        var entityValueRange = (CountableValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, entity);
        assertThat(entityValueRange.getSize()).isEqualTo(3);

        // Fetching from the descriptor does not include the null value
        var otherEntityValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractValuesFromEntity(solution, entity);
        assertThat(otherEntityValueRange.getSize()).isEqualTo(3);
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
            softly.assertThat(valueRangeManager.getMaximumValueRangeSize()).isEqualTo(valueCount);
            softly.assertThat(valueRangeManager.getApproximateValueCount()).isEqualTo(valueCount);
            softly.assertThat(valueRangeManager.getProblemScale())
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
            softly.assertThat(valueRangeManager.getMaximumValueRangeSize()).isEqualTo(0);
            softly.assertThat(valueRangeManager.getApproximateValueCount()).isEqualTo(0);
            softly.assertThat(valueRangeManager.getProblemScale())
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
            softly.assertThat(valueRangeManager.getMaximumValueRangeSize()).isEqualTo(1);
            softly.assertThat(valueRangeManager.getApproximateValueCount()).isEqualTo(1);
            softly.assertThat(valueRangeManager.getProblemScale())
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
            softly.assertThat(valueRangeManager.getMaximumValueRangeSize()).isEqualTo(3L);
            softly.assertThat(valueRangeManager.getApproximateValueCount())
                    .isEqualTo(variableCount * valueCount);
            softly.assertThat(valueRangeManager.getProblemScale())
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
            softly.assertThat(valueRangeManager.getMaximumValueRangeSize()).isEqualTo(4L);
            softly.assertThat(valueRangeManager.getApproximateValueCount()).isEqualTo(3L + 4L);
            softly.assertThat(valueRangeManager.getProblemScale())
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

            softly.assertThat(valueRangeManager.getMaximumValueRangeSize()).isEqualTo(3L);
            softly.assertThat(valueRangeManager.getApproximateValueCount()).isEqualTo(2L + 3L);
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
            softly.assertThat(valueRangeManager.getMaximumValueRangeSize()).isEqualTo(2L);
            softly.assertThat(valueRangeManager.getApproximateValueCount()).isEqualTo(2L);
            softly.assertThat(valueRangeManager.getProblemScale())
                    .isCloseTo(Math.log10(2), Percentage.withPercentage(1.0));
        });
    }

    @Test
    void problemScaleChained() {
        var anchorCount = 20;
        var entityCount = 500;
        var solutionDescriptor = TestdataChainedSolution.buildSolutionDescriptor();
        var solution = generateChainedSolution(anchorCount, entityCount);

        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);
        assertSoftly(softly -> {
            softly.assertThat(solutionDescriptor.getGenuineEntityCount(solution)).isEqualTo(entityCount);
            softly.assertThat(solutionDescriptor.getGenuineVariableCount(solution)).isEqualTo(entityCount * 2);
            softly.assertThat(valueRangeManager.getMaximumValueRangeSize())
                    .isEqualTo(entityCount + anchorCount);
            // 1 unchained value is inside the solution
            softly.assertThat(valueRangeManager.getApproximateValueCount())
                    .isEqualTo(entityCount + anchorCount + 1);
            softly.assertThat(valueRangeManager.getProblemScale())
                    .isCloseTo(MathUtils.getPossibleArrangementsScaledApproximateLog(MathUtils.LOG_PRECISION, 10, 500, 20)
                            / (double) MathUtils.LOG_PRECISION, Percentage.withPercentage(1.0));
        });
    }

    static TestdataChainedSolution generateChainedSolution(int anchorCount, int entityCount) {
        var solution = new TestdataChainedSolution("test solution");
        var anchorList = IntStream.range(0, anchorCount)
                .mapToObj(Integer::toString)
                .map(TestdataChainedAnchor::new).toList();
        solution.setChainedAnchorList(anchorList);
        var entityList = IntStream.range(0, entityCount)
                .mapToObj(Integer::toString)
                .map(TestdataChainedEntity::new).toList();
        solution.setChainedEntityList(entityList);
        solution.setUnchainedValueList(Collections.singletonList(new TestdataValue("v")));
        return solution;
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
            softly.assertThat(valueRangeManager.getMaximumValueRangeSize()).isEqualTo(valueCount);
            softly.assertThat(valueRangeManager.getApproximateValueCount()).isEqualTo(valueCount);
            softly.assertThat(valueRangeManager.getProblemScale())
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
            softly.assertThat(valueRangeManager.getMaximumValueRangeSize()).isEqualTo(valueCount);
            softly.assertThat(valueRangeManager.getApproximateValueCount()).isEqualTo(valueCount);
            softly.assertThat(valueRangeManager.getProblemScale()).isEqualTo(0.0);
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
            softly.assertThat(valueRangeManager.getMaximumValueRangeSize()).isEqualTo(valueCount);
            softly.assertThat(valueRangeManager.getApproximateValueCount()).isEqualTo(valueCount);
            softly.assertThat(valueRangeManager.getProblemScale())
                    .isCloseTo(Math.log10(2), Percentage.withPercentage(1.0));
        });
    }

    @Test
    void assertProblemScaleListIsApproximatelyProblemScaleChained() {
        var valueCount = 500;
        var entityCount = 20;

        var solutionDescriptor = TestdataListSolution.buildSolutionDescriptor();
        var listSolution = TestdataListSolution.generateUninitializedSolution(valueCount, entityCount);
        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, listSolution);
        var listPowerExponent = valueRangeManager.getProblemScale();

        var solutionDescriptorChained = TestdataChainedSolution.buildSolutionDescriptor();
        var solutionChained = generateChainedSolution(entityCount, valueCount);
        var valueRangeManagerChained = ValueRangeManager.of(solutionDescriptorChained, solutionChained);
        var chainedPowerExponent = valueRangeManagerChained.getProblemScale();

        // Since they are using different bases in calculation, some difference is expected,
        // but the numbers should be relatively (i.e. ~1%) close.
        assertThat(Math.pow(10, listPowerExponent))
                .isCloseTo(Math.pow(10, chainedPowerExponent), Percentage.withPercentage(1));
    }

}
