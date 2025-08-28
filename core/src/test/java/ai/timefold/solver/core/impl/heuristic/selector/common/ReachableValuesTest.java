package ai.timefold.solver.core.impl.heuristic.selector.common;

import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockScoreDirector;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingValue;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.multivar.TestdataMultiVarEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.multivar.TestdataMultiVarEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingSolution;

import org.junit.jupiter.api.Test;

class ReachableValuesTest {

    @Test
    void testReachableValuesByEntity() {
        var v1 = new TestdataListEntityProvidingValue("V1");
        var v2 = new TestdataListEntityProvidingValue("V2");
        var v3 = new TestdataListEntityProvidingValue("V3");
        var v4 = new TestdataListEntityProvidingValue("V4");
        var v5 = new TestdataListEntityProvidingValue("V5");
        var a = new TestdataListEntityProvidingEntity("A", List.of(v1, v2, v3), List.of(v1, v2));
        var b = new TestdataListEntityProvidingEntity("B", List.of(v2, v3), List.of(v3));
        var c = new TestdataListEntityProvidingEntity("C", List.of(v3, v4, v5), List.of(v4, v5));
        var solution = new TestdataListEntityProvidingSolution();
        solution.setEntityList(List.of(a, b, c));

        var scoreDirector = mockScoreDirector(TestdataListEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var solutionDescriptor = scoreDirector.getSolutionDescriptor();
        var entityDescriptor = solutionDescriptor.findEntityDescriptor(TestdataListEntityProvidingEntity.class);
        var reachableValues = scoreDirector.getValueRangeManager()
                .getReachableValues(entityDescriptor.getGenuineListVariableDescriptor());

        assertThat(reachableValues.extractEntitiesAsList(v1)).containsExactlyInAnyOrder(a);
        assertThat(reachableValues.extractEntitiesAsList(v2)).containsExactlyInAnyOrder(a, b);
        assertThat(reachableValues.extractEntitiesAsList(v3)).containsExactlyInAnyOrder(a, b, c);
        assertThat(reachableValues.extractEntitiesAsList(v4)).containsExactlyInAnyOrder(c);
        assertThat(reachableValues.extractEntitiesAsList(v5)).containsExactlyInAnyOrder(c);

        assertThat(reachableValues.isEntityReachable(v1, a)).isTrue();
        assertThat(reachableValues.isEntityReachable(v1, b)).isFalse();
        assertThat(reachableValues.isEntityReachable(v1, c)).isFalse();

        assertThat(reachableValues.matchesValueClass(v1)).isTrue();
        assertThat(reachableValues.matchesValueClass(a)).isFalse();
    }

    @Test
    void testReachableValuesByValue() {
        var v1 = new TestdataListEntityProvidingValue("V1");
        var v2 = new TestdataListEntityProvidingValue("V2");
        var v3 = new TestdataListEntityProvidingValue("V3");
        var v4 = new TestdataListEntityProvidingValue("V4");
        var v5 = new TestdataListEntityProvidingValue("V5");
        var a = new TestdataListEntityProvidingEntity("A", List.of(v1, v2, v3), List.of(v1, v2));
        var b = new TestdataListEntityProvidingEntity("B", List.of(v2, v3), List.of(v3));
        var c = new TestdataListEntityProvidingEntity("C", List.of(v3, v4, v5), List.of(v4, v5));
        var solution = new TestdataListEntityProvidingSolution();
        solution.setEntityList(List.of(a, b, c));

        var scoreDirector = mockScoreDirector(TestdataListEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var solutionDescriptor = scoreDirector.getSolutionDescriptor();
        var entityDescriptor = solutionDescriptor.findEntityDescriptor(TestdataListEntityProvidingEntity.class);
        var reachableValues = scoreDirector.getValueRangeManager()
                .getReachableValues(entityDescriptor.getGenuineListVariableDescriptor());

        assertThat(reachableValues.extractValuesAsList(v1)).containsExactlyInAnyOrder(v2, v3);
        assertThat(reachableValues.extractValuesAsList(v2)).containsExactlyInAnyOrder(v1, v3);
        assertThat(reachableValues.extractValuesAsList(v3)).containsExactlyInAnyOrder(v1, v2, v4, v5);
        assertThat(reachableValues.extractValuesAsList(v4)).containsExactlyInAnyOrder(v3, v5);
        assertThat(reachableValues.extractValuesAsList(v5)).containsExactlyInAnyOrder(v3, v4);

        // Only origin
        assertThat(reachableValues.isValueReachable(v1, v2)).isTrue();
        assertThat(reachableValues.isValueReachable(v1, v3)).isTrue();
        assertThat(reachableValues.isValueReachable(v1, v5)).isFalse();

        // Null value is not accepted because the setting allowUnassigned is false
        assertThat(reachableValues.isValueReachable(v1, null)).isFalse();
    }

    @Test
    void testUnassignedReachableValues() {
        var v1 = new TestdataValue("V1");
        var v2 = new TestdataValue("V2");
        var v3 = new TestdataValue("V3");
        var v4 = new TestdataValue("V4");
        var v5 = new TestdataValue("V5");
        var a = new TestdataAllowsUnassignedEntityProvidingEntity("A", List.of(v1, v2, v3), v1);
        var b = new TestdataAllowsUnassignedEntityProvidingEntity("B", List.of(v2, v3), v3);
        var c = new TestdataAllowsUnassignedEntityProvidingEntity("C", List.of(v3, v4, v5), v4);
        var solution = new TestdataAllowsUnassignedEntityProvidingSolution();
        solution.setEntityList(List.of(a, b, c));

        var scoreDirector = mockScoreDirector(TestdataAllowsUnassignedEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var solutionDescriptor = scoreDirector.getSolutionDescriptor();
        var entityDescriptor = solutionDescriptor.findEntityDescriptor(TestdataAllowsUnassignedEntityProvidingEntity.class);
        var reachableValues = scoreDirector.getValueRangeManager()
                .getReachableValues(entityDescriptor.getGenuineVariableDescriptorList().get(0));

        assertThat(reachableValues.extractValuesAsList(v1)).containsExactlyInAnyOrder(v2, v3);
        assertThat(reachableValues.extractValuesAsList(v2)).containsExactlyInAnyOrder(v1, v3);
        assertThat(reachableValues.extractValuesAsList(v3)).containsExactlyInAnyOrder(v1, v2, v4, v5);
        assertThat(reachableValues.extractValuesAsList(v4)).containsExactlyInAnyOrder(v3, v5);
        assertThat(reachableValues.extractValuesAsList(v5)).containsExactlyInAnyOrder(v3, v4);

        // Only origin
        assertThat(reachableValues.isValueReachable(v1, v2)).isTrue();
        assertThat(reachableValues.isValueReachable(v1, v3)).isTrue();
        assertThat(reachableValues.isValueReachable(v1, v5)).isFalse();

        // Null value is not accepted because the setting allowUnassigned is false
        assertThat(reachableValues.isValueReachable(v1, null)).isTrue();
    }

    @Test
    void testListVarReachableEntities() {
        var v1 = new TestdataListEntityProvidingValue("V1");
        var v2 = new TestdataListEntityProvidingValue("V2");
        var v3 = new TestdataListEntityProvidingValue("V3");
        var v4 = new TestdataListEntityProvidingValue("V4");
        var v5 = new TestdataListEntityProvidingValue("V5");
        var e1 = new TestdataListEntityProvidingEntity("A", List.of(v1, v2), List.of(v1, v2));
        var e2 = new TestdataListEntityProvidingEntity("B", List.of(v2, v3), List.of(v3));
        var e3 = new TestdataListEntityProvidingEntity("C", List.of(v3, v4, v5), List.of(v4, v5));
        var solution = new TestdataListEntityProvidingSolution();
        solution.setEntityList(List.of(e1, e2, e3));

        var scoreDirector = mockScoreDirector(TestdataListEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        assertThat(scoreDirector.getValueRangeManager()
                .getReachableEntities(TestdataListEntityProvidingEntity.buildEntityDescriptor()).isReachable(e1, e2)).isTrue();
        assertThat(scoreDirector.getValueRangeManager()
                .getReachableEntities(TestdataListEntityProvidingEntity.buildEntityDescriptor()).isReachable(e1, e3)).isFalse();
        assertThat(scoreDirector.getValueRangeManager()
                .getReachableEntities(TestdataListEntityProvidingEntity.buildEntityDescriptor()).isReachable(e2, e1)).isTrue();
        assertThat(scoreDirector.getValueRangeManager()
                .getReachableEntities(TestdataListEntityProvidingEntity.buildEntityDescriptor()).isReachable(e2, e3)).isTrue();
        assertThat(scoreDirector.getValueRangeManager()
                .getReachableEntities(TestdataListEntityProvidingEntity.buildEntityDescriptor()).isReachable(e3, e1)).isFalse();
        assertThat(scoreDirector.getValueRangeManager()
                .getReachableEntities(TestdataListEntityProvidingEntity.buildEntityDescriptor()).isReachable(e3, e2)).isTrue();
    }

    @Test
    void testSingleBasicVarReachableEntities() {
        var v1 = new TestdataValue("V1");
        var v2 = new TestdataValue("V2");
        var v3 = new TestdataValue("V3");
        var v4 = new TestdataValue("V4");
        var v5 = new TestdataValue("V5");
        var e1 = new TestdataAllowsUnassignedEntityProvidingEntity("A", List.of(v1, v2), v1);
        var e2 = new TestdataAllowsUnassignedEntityProvidingEntity("B", List.of(v2, v3), v3);
        var e3 = new TestdataAllowsUnassignedEntityProvidingEntity("C", List.of(v3, v4, v5), v4);
        var solution = new TestdataAllowsUnassignedEntityProvidingSolution();
        solution.setEntityList(List.of(e1, e2, e3));

        var scoreDirector = mockScoreDirector(TestdataAllowsUnassignedEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        assertThat(scoreDirector.getValueRangeManager()
                .getReachableEntities(TestdataAllowsUnassignedEntityProvidingEntity.buildEntityDescriptor())
                .isReachable(e1, e2)).isTrue();
        assertThat(scoreDirector.getValueRangeManager()
                .getReachableEntities(TestdataAllowsUnassignedEntityProvidingEntity.buildEntityDescriptor())
                .isReachable(e1, e3)).isFalse();
        assertThat(scoreDirector.getValueRangeManager()
                .getReachableEntities(TestdataAllowsUnassignedEntityProvidingEntity.buildEntityDescriptor())
                .isReachable(e2, e1)).isTrue();
        assertThat(scoreDirector.getValueRangeManager()
                .getReachableEntities(TestdataAllowsUnassignedEntityProvidingEntity.buildEntityDescriptor())
                .isReachable(e2, e3)).isTrue();
        assertThat(scoreDirector.getValueRangeManager()
                .getReachableEntities(TestdataAllowsUnassignedEntityProvidingEntity.buildEntityDescriptor())
                .isReachable(e3, e1)).isFalse();
        assertThat(scoreDirector.getValueRangeManager()
                .getReachableEntities(TestdataAllowsUnassignedEntityProvidingEntity.buildEntityDescriptor())
                .isReachable(e3, e2)).isTrue();
    }

    @Test
    void testMultiBasicVarReachableEntities() {
        var v1 = new TestdataValue("V1");
        var v2 = new TestdataValue("V2");
        var v3 = new TestdataValue("V3");
        var v4 = new TestdataValue("V4");
        var v5 = new TestdataValue("V5");
        var e1 = new TestdataMultiVarEntityProvidingEntity("A", List.of(v1, v2), List.of(v1, v2));
        var e2 = new TestdataMultiVarEntityProvidingEntity("B", List.of(v2, v3), List.of(v2, v3));
        var e3 = new TestdataMultiVarEntityProvidingEntity("C", List.of(v3, v4, v5), List.of(v3, v4, v5));
        var solution = new TestdataMultiVarEntityProvidingSolution();
        solution.setEntityList(List.of(e1, e2, e3));

        var scoreDirector = mockScoreDirector(TestdataMultiVarEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        assertThat(scoreDirector.getValueRangeManager()
                .getReachableEntities(TestdataMultiVarEntityProvidingEntity.buildEntityDescriptor())
                .isReachable(e1, e2)).isTrue();
        assertThat(scoreDirector.getValueRangeManager()
                .getReachableEntities(TestdataMultiVarEntityProvidingEntity.buildEntityDescriptor())
                .isReachable(e1, e3)).isFalse();
        assertThat(scoreDirector.getValueRangeManager()
                .getReachableEntities(TestdataMultiVarEntityProvidingEntity.buildEntityDescriptor())
                .isReachable(e2, e1)).isTrue();
        assertThat(scoreDirector.getValueRangeManager()
                .getReachableEntities(TestdataMultiVarEntityProvidingEntity.buildEntityDescriptor())
                .isReachable(e2, e3)).isTrue();
        assertThat(scoreDirector.getValueRangeManager()
                .getReachableEntities(TestdataMultiVarEntityProvidingEntity.buildEntityDescriptor())
                .isReachable(e3, e1)).isFalse();
        assertThat(scoreDirector.getValueRangeManager()
                .getReachableEntities(TestdataMultiVarEntityProvidingEntity.buildEntityDescriptor())
                .isReachable(e3, e2)).isTrue();
    }
}
