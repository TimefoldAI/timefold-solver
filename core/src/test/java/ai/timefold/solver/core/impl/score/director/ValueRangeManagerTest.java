package ai.timefold.solver.core.impl.score.director;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
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
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityWithDoubleValueRange;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListSolutionWithDoubleValueRange;
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
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.TestdataEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.TestdataEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.composite.TestdataCompositeEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.composite.TestdataCompositeEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.composite.TestdataAllowsUnassignedCompositeEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.composite.TestdataAllowsUnassignedCompositeEntityProvidingSolution;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ValueRangeManagerTest {

    @Test
    void extractValueFromSolutionUnassignedBasicVariable() {
        var valueRangeManager = new ValueRangeManager<TestdataAllowsUnassignedSolution>();
        var solution = TestdataAllowsUnassignedSolution.generateSolution(2, 2);
        var valueRangeDescriptor = TestdataAllowsUnassignedEntity.buildVariableDescriptorForValue()
                .getValueRangeDescriptor();

        // The value range manager will add the null value
        // 2 distinct values
        assertThat(valueRangeManager.countOnSolution(valueRangeDescriptor, solution)).isEqualTo(3);
        var valueRange = (CountableValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(3);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractValueRange(solution, null);
        assertThat(otherValueRange.getSize()).isEqualTo(2);
    }

    @Test
    void extractValueFromSolutionCompositeUnassignedBasicVariable() {
        var valueRangeManager = new ValueRangeManager<TestdataAllowsUnassignedCompositeSolution>();
        var solution = TestdataAllowsUnassignedCompositeSolution.generateSolution(2, 2);
        var valueRangeDescriptor = TestdataAllowsUnassignedCompositeEntity.buildVariableDescriptorForValue()
                .getValueRangeDescriptor();

        // The value range manager will not add the null value
        // valueRange1 [v1, v2] -> 2 distinct values
        // valueRange2 [v3, v4] -> 2 distinct values
        assertThat(valueRangeManager.countOnSolution(valueRangeDescriptor, solution)).isEqualTo(4);
        var valueRange = (CountableValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(4);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractValueRange(solution, null);
        assertThat(otherValueRange.getSize()).isEqualTo(4);
    }

    @Test
    void extractValueFromSolutionAssignedBasicVariable() {
        var valueRangeManager = new ValueRangeManager<TestdataSolution>();
        var solution = TestdataSolution.generateSolution(2, 2);
        var valueRangeDescriptor = TestdataEntity.buildVariableDescriptorForValue()
                .getValueRangeDescriptor();

        // The value range manager will not add the null value
        // 2 distinct values
        assertThat(valueRangeManager.countOnSolution(valueRangeDescriptor, solution)).isEqualTo(2);
        var valueRange = (CountableValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(2);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractValueRange(solution, null);
        assertThat(otherValueRange.getSize()).isEqualTo(2);
    }

    @Test
    void extractValueFromSolutionCompositeAssignedBasicVariable() {
        var valueRangeManager = new ValueRangeManager<TestdataCompositeSolution>();
        var solution = TestdataCompositeSolution.generateSolution(2, 2);
        var valueRangeDescriptor = TestdataCompositeSolution.buildSolutionDescriptor()
                .findEntityDescriptor(TestdataCompositeEntity.class)
                .getGenuineVariableDescriptor("value")
                .getValueRangeDescriptor();

        // The value range manager will not add the null value
        // valueRange1 [v0, v1] -> 2 distinct values
        // valueRange2 [v2, v3] -> 2 distinct values
        assertThat(valueRangeManager.countOnSolution(valueRangeDescriptor, solution)).isEqualTo(4);
        var valueRange = (CountableValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(4);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractValueRange(solution, null);
        assertThat(otherValueRange.getSize()).isEqualTo(4);
    }

    @Test
    void extractValueFromEntityUnassignedBasicVariable() {
        var valueRangeManager = new ValueRangeManager<TestdataAllowsUnassignedEntityProvidingSolution>();
        var solution = TestdataAllowsUnassignedEntityProvidingSolution.generateSolution();
        var valueRangeDescriptor = TestdataAllowsUnassignedEntityProvidingEntity.buildVariableDescriptorForValue()
                .getValueRangeDescriptor();

        // The value range manager will add the null value
        // Two entities: e1(v1, v2) and e2(v1, v3) -> 3 distinct values
        assertThat(valueRangeManager.countOnSolution(valueRangeDescriptor, solution)).isEqualTo(4);
        var valueRange = (CountableValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(4);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractValueRange(solution, null);
        assertThat(otherValueRange.getSize()).isEqualTo(3);

        // The value range manager will add the null value
        // e1(v1, v2) -> 2 distinct values
        var entity = solution.getEntityList().get(0);
        assertThat(valueRangeManager.countOnEntity(valueRangeDescriptor, entity)).isEqualTo(3);
        var entityValueRange = (CountableValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, entity);
        assertThat(entityValueRange.getSize()).isEqualTo(3);

        // Fetching from the descriptor does not include the null value
        var otherEntityValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractValueRange(null, entity);
        assertThat(otherEntityValueRange.getSize()).isEqualTo(2);
    }

    @Test
    void extractValueFromEntityCompositeUnassignedBasicVariable() {
        var valueRangeManager = new ValueRangeManager<TestdataAllowsUnassignedCompositeEntityProvidingSolution>();
        var solution = TestdataAllowsUnassignedCompositeEntityProvidingSolution.generateSolution();
        var valueRangeDescriptor = TestdataAllowsUnassignedCompositeEntityProvidingEntity.buildVariableDescriptorForValue()
                .getValueRangeDescriptor();

        // The value range manager will not add the null value
        // e1([v1, v2], [v1, v4]) -> 3 distinct values
        // e2([v1, v3], [v1, v5]) -> 3 distinct values
        // The composite range returns all values from both ranges -> 6 values
        assertThat(valueRangeManager.countOnSolution(valueRangeDescriptor, solution)).isEqualTo(6);
        var valueRange = (CountableValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(6);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractValueRange(solution, null);
        assertThat(otherValueRange.getSize()).isEqualTo(6);

        // The value range manager will not add the null value
        // e1([v1, v2], [v1, v3]) -> 3 distinct values
        // The composite range returns all values from both ranges -> 4 values
        var entity = solution.getEntityList().get(0);
        assertThat(valueRangeManager.countOnEntity(valueRangeDescriptor, entity)).isEqualTo(4);
        var entityValueRange = (CountableValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, entity);
        assertThat(entityValueRange.getSize()).isEqualTo(4);

        // Fetching from the descriptor does not include the null value
        var otherEntityValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractValueRange(null, entity);
        assertThat(otherEntityValueRange.getSize()).isEqualTo(4);
    }

    @Test
    void extractValueFromEntityAssignedBasicVariable() {
        var valueRangeManager = new ValueRangeManager<TestdataEntityProvidingSolution>();
        var solution = TestdataEntityProvidingSolution.generateSolution();
        var valueRangeDescriptor = TestdataEntityProvidingEntity.buildVariableDescriptorForValue()
                .getValueRangeDescriptor();

        // The value range manager will add the null value
        // Two entities: e1(v1, v2) and e2(v1, v3) -> 3 distinct values
        assertThat(valueRangeManager.countOnSolution(valueRangeDescriptor, solution)).isEqualTo(3);
        var valueRange = (CountableValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(3);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractValueRange(solution, null);
        assertThat(otherValueRange.getSize()).isEqualTo(3);

        // The value range manager will add the null value
        // e1(v1, v2) -> 2 distinct values
        var entity = solution.getEntityList().get(0);
        assertThat(valueRangeManager.countOnEntity(valueRangeDescriptor, entity)).isEqualTo(2);
        var entityValueRange = (CountableValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, entity);
        assertThat(entityValueRange.getSize()).isEqualTo(2);

        // Fetching from the descriptor does not include the null value
        var otherEntityValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractValueRange(null, entity);
        assertThat(otherEntityValueRange.getSize()).isEqualTo(2);
    }

    @Test
    void extractValueFromEntityCompositeAssignedBasicVariable() {
        var valueRangeManager = new ValueRangeManager<TestdataCompositeEntityProvidingSolution>();
        var solution = TestdataCompositeEntityProvidingSolution.generateSolution();
        var valueRangeDescriptor = TestdataCompositeEntityProvidingEntity.buildVariableDescriptorForValue()
                .getValueRangeDescriptor();

        // The value range manager will not add the null value
        // e1([v1, v2], [v1, v4]) -> 3 distinct values
        // e2([v1, v3], [v1, v5]) -> 3 distinct values
        // The composite range returns all values from both ranges -> 6 values
        assertThat(valueRangeManager.countOnSolution(valueRangeDescriptor, solution)).isEqualTo(6);
        var valueRange = (CountableValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(6);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractValueRange(solution, null);
        assertThat(otherValueRange.getSize()).isEqualTo(6);

        // The value range manager will not add the null value
        // e1([v1, v2], [v1, v4]) -> 3 distinct values
        // The composite range returns all values from both ranges -> 4 values
        var entity = solution.getEntityList().get(0);
        assertThat(valueRangeManager.countOnEntity(valueRangeDescriptor, entity)).isEqualTo(4);
        var entityValueRange = (CountableValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, entity);
        assertThat(entityValueRange.getSize()).isEqualTo(4);

        // Fetching from the descriptor does not include the null value
        var otherEntityValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractValueRange(null, entity);
        assertThat(otherEntityValueRange.getSize()).isEqualTo(4);
    }

    @Test
    void extractValueFromSolutionUnassignedListVariable() {
        var valueRangeManager = new ValueRangeManager<TestdataAllowsUnassignedValuesListSolution>();
        var solution = TestdataAllowsUnassignedValuesListSolution.generateUninitializedSolution(2, 2);
        var valueRangeDescriptor = TestdataAllowsUnassignedValuesListEntity.buildVariableDescriptorForValueList()
                .getValueRangeDescriptor();

        // The value range manager will not add the null value because it is a list variable
        // 2 distinct values
        assertThat(valueRangeManager.countOnSolution(valueRangeDescriptor, solution)).isEqualTo(2);
        var valueRange = (CountableValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(2);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractValueRange(solution, null);
        assertThat(otherValueRange.getSize()).isEqualTo(2);
    }

    @Test
    void extractValueFromSolutionCompositeUnassignedListVariable() {
        var valueRangeManager = new ValueRangeManager<TestdataAllowsUnassignedCompositeListSolution>();
        var solution = TestdataAllowsUnassignedCompositeListSolution.generateSolution(2, 2);
        var valueRangeDescriptor = TestdataAllowsUnassignedCompositeListEntity.buildVariableDescriptorForValueList()
                .getValueRangeDescriptor();

        // The value range manager will not add the null value because it is a list variable
        // valueRange1 [v1, v2] -> 2 distinct values
        // valueRange2 [v3, v4] -> 2 distinct values
        assertThat(valueRangeManager.countOnSolution(valueRangeDescriptor, solution)).isEqualTo(4);
        var valueRange = (CountableValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(4);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractValueRange(solution, null);
        assertThat(otherValueRange.getSize()).isEqualTo(4);
    }

    @Test
    void extractValueFromSolutionAssignedListVariable() {
        var valueRangeManager = new ValueRangeManager<TestdataListSolution>();
        var solution = TestdataListSolution.generateUninitializedSolution(2, 2);
        var valueRangeDescriptor = TestdataListSolution.buildSolutionDescriptor()
                .findEntityDescriptor(TestdataListEntity.class)
                .getGenuineVariableDescriptor("valueList")
                .getValueRangeDescriptor();
        // The value range manager will not add the null value
        // 2 distinct values
        assertThat(valueRangeManager.countOnSolution(valueRangeDescriptor, solution)).isEqualTo(2);
        var valueRange = (CountableValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(2);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractValueRange(solution, null);
        assertThat(otherValueRange.getSize()).isEqualTo(2);
    }

    @Test
    void extractValueFromSolutionCompositeAssignedListVariable() {
        var valueRangeManager = new ValueRangeManager<TestdataListCompositeSolution>();
        var solution = TestdataListCompositeSolution.generateSolution(2, 2);
        var valueRangeDescriptor = TestdataListCompositeEntity.buildVariableDescriptorForValueList()
                .getValueRangeDescriptor();

        // The value range manager will not add the null value
        // valueRange1 [v0, v1] -> 2 distinct values
        // valueRange2 [v2, v3] -> 2 distinct values
        assertThat(valueRangeManager.countOnSolution(valueRangeDescriptor, solution)).isEqualTo(4);
        var valueRange = (CountableValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(4);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractValueRange(solution, null);
        assertThat(otherValueRange.getSize()).isEqualTo(4);
    }

    @Disabled("Temporarily disabled")
    @Test
    void extractValueFromEntityUnassignedListVariable() {
        var valueRangeManager = new ValueRangeManager<TestdataListUnassignedEntityProvidingSolution>();
        var solution = TestdataListUnassignedEntityProvidingSolution.generateSolution();
        var valueRangeDescriptor = TestdataListUnassignedEntityProvidingEntity.buildVariableDescriptorForValueList()
                .getValueRangeDescriptor();

        // The value range manager will not add the null value because it is a list variable
        // Two entities: e1(v1, v2) and e2(v1, v3) -> 3 distinct values
        assertThat(valueRangeManager.countOnSolution(valueRangeDescriptor, solution)).isEqualTo(3);
        var valueRange = (CountableValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(3);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractValueRange(solution, null);
        assertThat(otherValueRange.getSize()).isEqualTo(3);

        // The value range manager will not add the null value because it is a list variable
        // e1(v1, v2) -> 2 distinct values
        var entity = solution.getEntityList().get(0);
        assertThat(valueRangeManager.countOnEntity(valueRangeDescriptor, entity)).isEqualTo(2);
        var entityValueRange = (CountableValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, entity);
        assertThat(entityValueRange.getSize()).isEqualTo(2);

        // Fetching from the descriptor does not include the null value
        var otherEntityValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractValueRange(null, entity);
        assertThat(otherEntityValueRange.getSize()).isEqualTo(2);
    }

    @Disabled("Temporarily disabled")
    @Test
    void extractValueFromEntityCompositeUnassignedListVariable() {
        var valueRangeManager = new ValueRangeManager<TestdataListUnassignedCompositeEntityProvidingSolution>();
        var solution = TestdataListUnassignedCompositeEntityProvidingSolution.generateSolution();
        var valueRangeDescriptor = TestdataListUnassignedCompositeEntityProvidingEntity.buildVariableDescriptorForValueList()
                .getValueRangeDescriptor();

        // The value range manager will not add the null value because it is a list variable
        // e1([v1, v2], [v1, v3]) -> 3 distinct values
        // e2([v1, v4], [v1, v5]) -> 3 distinct values
        // The composite range returns all values from both ranges -> 6 values
        assertThat(valueRangeManager.countOnSolution(valueRangeDescriptor, solution)).isEqualTo(6);
        var valueRange = (CountableValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(6);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractValueRange(solution, null);
        assertThat(otherValueRange.getSize()).isEqualTo(6);

        // The value range manager will not add the null value because it is a list variable
        // e1([v1, v2], [v1, v3]) -> 3 distinct values
        // The composite range returns all values from both ranges -> 4 values
        var entity = solution.getEntityList().get(0);
        assertThat(valueRangeManager.countOnEntity(valueRangeDescriptor, entity)).isEqualTo(4);
        var entityValueRange = (CountableValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, entity);
        assertThat(entityValueRange.getSize()).isEqualTo(4);

        // Fetching from the descriptor does not include the null value
        var otherEntityValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractValueRange(null, entity);
        assertThat(otherEntityValueRange.getSize()).isEqualTo(4);
    }

    @Disabled("Temporarily disabled")
    @Test
    void extractValueFromEntityAssignedListVariable() {
        var valueRangeManager = new ValueRangeManager<TestdataListEntityProvidingSolution>();
        var solution = TestdataListEntityProvidingSolution.generateSolution();
        var valueRangeDescriptor = TestdataListEntityProvidingEntity.buildVariableDescriptorForValueList()
                .getValueRangeDescriptor();

        // The value range manager will not add the null value because it is a list variable
        // Two entities: e1(v1, v2) and e2(v1, v3) -> 3 distinct values
        assertThat(valueRangeManager.countOnSolution(valueRangeDescriptor, solution)).isEqualTo(3);
        var valueRange = (CountableValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(3);

        // Fetching from the descriptor does not include the null value because it is a list variable
        var otherValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractValueRange(solution, null);
        assertThat(otherValueRange.getSize()).isEqualTo(3);

        // The value range manager will not add the null value because it is a list variable
        // e1(v1, v2) -> 2 distinct values
        var entity = solution.getEntityList().get(0);
        assertThat(valueRangeManager.countOnEntity(valueRangeDescriptor, entity)).isEqualTo(2);
        var entityValueRange = (CountableValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, entity);
        assertThat(entityValueRange.getSize()).isEqualTo(2);

        // Fetching from the descriptor does not include the null value
        var otherEntityValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractValueRange(null, entity);
        assertThat(otherEntityValueRange.getSize()).isEqualTo(2);
    }

    @Disabled("Temporarily disabled")
    @Test
    void extractValueFromEntityCompositeAssignedListVariable() {
        var valueRangeManager = new ValueRangeManager<TestdataListCompositeEntityProvidingSolution>();
        var solution = TestdataListCompositeEntityProvidingSolution.generateSolution();
        var valueRangeDescriptor = TestdataListCompositeEntityProvidingEntity.buildVariableDescriptorForValueList()
                .getValueRangeDescriptor();

        // The value range manager will not add the null value because it is a list variable
        // e1([v1, v2], [v1, v3]) -> 3 distinct values
        // e2([v1, v4], [v1, v5]) -> 3 distinct values
        // The composite range returns all values from both ranges -> 6 values
        assertThat(valueRangeManager.countOnSolution(valueRangeDescriptor, solution)).isEqualTo(6);
        var valueRange = (CountableValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(6);

        // Fetching from the descriptor does not include the null value because it is a list variable
        var otherValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractValueRange(solution, null);
        assertThat(otherValueRange.getSize()).isEqualTo(6);

        // The value range manager will add the null value because it is a list variable
        // e1([v1, v2], [v1, v3]) -> 3 distinct values
        // The composite range returns all values from both ranges -> 4 values
        var entity = solution.getEntityList().get(0);
        assertThat(valueRangeManager.countOnEntity(valueRangeDescriptor, entity)).isEqualTo(4);
        var entityValueRange = (CountableValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, entity);
        assertThat(entityValueRange.getSize()).isEqualTo(4);

        // Fetching from the descriptor does not include the null value
        var otherEntityValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractValueRange(null, entity);
        assertThat(otherEntityValueRange.getSize()).isEqualTo(4);
    }

    @Test
    void testNonCountableValueRange() {
        var valueRangeManager = new ValueRangeManager<TestdataListSolutionWithDoubleValueRange>();
        var solution = TestdataListSolutionWithDoubleValueRange.generateSolution();
        var valueRangeDescriptor = TestdataListEntityWithDoubleValueRange.buildVariableDescriptorForValueList()
                .getValueRangeDescriptor();

        // The value range manager will not add the null value because because it is a list variable
        assertThatCode(() -> valueRangeManager.countOnSolution(valueRangeDescriptor, solution))
                .hasMessageContaining("is not countable.");

    }
}
