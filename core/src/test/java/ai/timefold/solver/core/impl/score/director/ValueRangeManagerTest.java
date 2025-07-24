package ai.timefold.solver.core.impl.score.director;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
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
        var solution = TestdataAllowsUnassignedSolution.generateSolution(2, 2);
        var valueRangeDescriptor = TestdataAllowsUnassignedEntity.buildVariableDescriptorForValue()
                .getValueRangeDescriptor();
        var solutionDescriptor = valueRangeDescriptor.getVariableDescriptor().getEntityDescriptor().getSolutionDescriptor();
        var valueRangeManager = createValueRangeManager(solution, solutionDescriptor);

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
        var valueRangeManager = createValueRangeManager(solution, solutionDescriptor);

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
        var valueRangeManager = createValueRangeManager(solution, solutionDescriptor);

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
        var valueRangeManager = createValueRangeManager(solution, solutionDescriptor);

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
        var valueRangeManager = createValueRangeManager(solution, solutionDescriptor);

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
        var valueRangeManager = createValueRangeManager(solution, solutionDescriptor);

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
        var valueRangeManager = createValueRangeManager(solution, solutionDescriptor);

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
        var valueRangeManager = createValueRangeManager(solution, solutionDescriptor);

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
        var valueRangeManager = createValueRangeManager(solution, solutionDescriptor);

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
        var valueRangeManager = createValueRangeManager(solution, solutionDescriptor);

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
        var valueRangeManager = createValueRangeManager(solution, solutionDescriptor);

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
        var valueRangeManager = createValueRangeManager(solution, solutionDescriptor);

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

    @Disabled("Temporarily disabled")
    @Test
    void extractValueFromEntityUnassignedListVariable() {
        var solution = TestdataListUnassignedEntityProvidingSolution.generateSolution();
        var valueRangeDescriptor = TestdataListUnassignedEntityProvidingEntity.buildVariableDescriptorForValueList()
                .getValueRangeDescriptor();
        var solutionDescriptor = valueRangeDescriptor.getVariableDescriptor().getEntityDescriptor().getSolutionDescriptor();
        var valueRangeManager = createValueRangeManager(solution, solutionDescriptor);

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

    @Disabled("Temporarily disabled")
    @Test
    void extractValueFromEntityCompositeUnassignedListVariable() {
        var solution = TestdataListUnassignedCompositeEntityProvidingSolution.generateSolution();
        var valueRangeDescriptor = TestdataListUnassignedCompositeEntityProvidingEntity.buildVariableDescriptorForValueList()
                .getValueRangeDescriptor();
        var solutionDescriptor = valueRangeDescriptor.getVariableDescriptor().getEntityDescriptor().getSolutionDescriptor();
        var valueRangeManager = createValueRangeManager(solution, solutionDescriptor);

        // The value range manager will not add the null value because it is a list variable
        // e1([v1, v2], [v1, v3]) -> 3 distinct values
        // e2([v1, v4], [v1, v5]) -> 3 distinct values
        // The composite range returns all values from both ranges -> 6 values
        assertThat(valueRangeManager.countOnSolution(valueRangeDescriptor, solution)).isEqualTo(6);
        var valueRange = (CountableValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(6);

        // Fetching from the descriptor does not include the null value
        var otherValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractAllValues(solution);
        assertThat(otherValueRange.getSize()).isEqualTo(6);

        // The value range manager will not add the null value because it is a list variable
        // e1([v1, v2], [v1, v3]) -> 3 distinct values
        // The composite range returns all values from both ranges -> 4 values
        var entity = solution.getEntityList().get(0);
        assertThat(valueRangeManager.countOnEntity(valueRangeDescriptor, entity)).isEqualTo(4);
        var entityValueRange = (CountableValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, entity);
        assertThat(entityValueRange.getSize()).isEqualTo(4);

        // Fetching from the descriptor does not include the null value
        var otherEntityValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractValuesFromEntity(solution, entity);
        assertThat(otherEntityValueRange.getSize()).isEqualTo(4);
    }

    @Disabled("Temporarily disabled")
    @Test
    void extractValueFromEntityAssignedListVariable() {
        var solution = TestdataListEntityProvidingSolution.generateSolution();
        var valueRangeDescriptor = TestdataListEntityProvidingEntity.buildVariableDescriptorForValueList()
                .getValueRangeDescriptor();
        var solutionDescriptor = valueRangeDescriptor.getVariableDescriptor().getEntityDescriptor().getSolutionDescriptor();
        var valueRangeManager = createValueRangeManager(solution, solutionDescriptor);

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

    @Disabled("Temporarily disabled")
    @Test
    void extractValueFromEntityCompositeAssignedListVariable() {
        var solution = TestdataListCompositeEntityProvidingSolution.generateSolution();
        var valueRangeDescriptor = TestdataListCompositeEntityProvidingEntity.buildVariableDescriptorForValueList()
                .getValueRangeDescriptor();
        var solutionDescriptor = valueRangeDescriptor.getVariableDescriptor().getEntityDescriptor().getSolutionDescriptor();
        var valueRangeManager = createValueRangeManager(solution, solutionDescriptor);

        // The value range manager will not add the null value because it is a list variable
        // e1([v1, v2], [v1, v3]) -> 3 distinct values
        // e2([v1, v4], [v1, v5]) -> 3 distinct values
        // The composite range returns all values from both ranges -> 6 values
        assertThat(valueRangeManager.countOnSolution(valueRangeDescriptor, solution)).isEqualTo(6);
        var valueRange = (CountableValueRange<?>) valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
        assertThat(valueRange.getSize()).isEqualTo(6);

        // Fetching from the descriptor does not include the null value because it is a list variable
        var otherValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractAllValues(solution);
        assertThat(otherValueRange.getSize()).isEqualTo(6);

        // The value range manager will add the null value because it is a list variable
        // e1([v1, v2], [v1, v3]) -> 3 distinct values
        // The composite range returns all values from both ranges -> 4 values
        var entity = solution.getEntityList().get(0);
        assertThat(valueRangeManager.countOnEntity(valueRangeDescriptor, entity)).isEqualTo(4);
        var entityValueRange = (CountableValueRange<?>) valueRangeManager.getFromEntity(valueRangeDescriptor, entity);
        assertThat(entityValueRange.getSize()).isEqualTo(4);

        // Fetching from the descriptor does not include the null value
        var otherEntityValueRange = (CountableValueRange<?>) valueRangeDescriptor.extractValuesFromEntity(solution, entity);
        assertThat(otherEntityValueRange.getSize()).isEqualTo(4);
    }

    private <Solution_> ValueRangeManager<Solution_> createValueRangeManager(Solution_ solution,
            SolutionDescriptor<Solution_> solutionDescriptor) {
        var valueRangeManager = new ValueRangeManager<Solution_>(solutionDescriptor);
        valueRangeManager.reset(solution);
        return valueRangeManager;
    }
}
