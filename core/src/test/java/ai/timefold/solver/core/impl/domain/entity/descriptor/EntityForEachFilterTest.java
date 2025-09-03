package ai.timefold.solver.core.impl.domain.entity.descriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Duration;
import java.util.List;

import ai.timefold.solver.core.impl.domain.variable.declarative.ChangedVariableNotifier;
import ai.timefold.solver.core.impl.domain.variable.declarative.EntityConsistencyStateDemand;
import ai.timefold.solver.core.impl.domain.variable.supply.ScoreDirectorIndependentSupplyManager;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.list.pinned.noshadows.TestdataPinnedNoShadowsListEntity;
import ai.timefold.solver.core.testdomain.list.pinned.noshadows.TestdataPinnedNoShadowsListSolution;
import ai.timefold.solver.core.testdomain.list.pinned.noshadows.TestdataPinnedNoShadowsListValue;
import ai.timefold.solver.core.testdomain.shadow.basic.TestdataBasicVarEntity;
import ai.timefold.solver.core.testdomain.shadow.basic.TestdataBasicVarSolution;
import ai.timefold.solver.core.testdomain.shadow.basic.TestdataBasicVarValue;
import ai.timefold.solver.core.testdomain.shadow.concurrent.TestdataConcurrentEntity;
import ai.timefold.solver.core.testdomain.shadow.concurrent.TestdataConcurrentSolution;
import ai.timefold.solver.core.testdomain.shadow.concurrent.TestdataConcurrentValue;

import org.junit.jupiter.api.Test;

class EntityForEachFilterTest {

    @Test
    void filtersForBasicEntities() {
        var solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        var entityDescriptor = solutionDescriptor.getEntityDescriptorStrict(TestdataEntity.class);
        var entityForEachFilter = new EntityForEachFilter(entityDescriptor);

        var supplyManager = new ScoreDirectorIndependentSupplyManager();
        var assignedAndConsistentPredicate = entityForEachFilter.getAssignedAndConsistentPredicate(supplyManager);
        var consistentPredicate = entityForEachFilter.getConsistentPredicate(supplyManager);

        assertThat(consistentPredicate).isNull();

        var entity = new TestdataEntity("entity");
        var value = new TestdataValue("value");

        entity.setValue(null);

        assertThat(assignedAndConsistentPredicate).rejects(entity);

        entity.setValue(value);

        assertThat(assignedAndConsistentPredicate).accepts(entity);
    }

    @Test
    void filtersForListValueEntities() {
        var solutionDescriptor = TestdataListSolution.buildSolutionDescriptor();
        var entityDescriptor = solutionDescriptor.getEntityDescriptorStrict(TestdataListValue.class);
        var entityForEachFilter = new EntityForEachFilter(entityDescriptor);

        var supplyManager = new ScoreDirectorIndependentSupplyManager();
        var assignedAndConsistentPredicate = entityForEachFilter.getAssignedAndConsistentPredicate(supplyManager);
        var consistentPredicate = entityForEachFilter.getConsistentPredicate(supplyManager);

        assertThat(consistentPredicate).isNull();

        var entity = new TestdataListEntity("entity");
        var value = new TestdataListValue("value");

        entity.setValueList(List.of());
        value.setEntity(null);

        assertThat(assignedAndConsistentPredicate).rejects(value);

        entity.setValueList(List.of(value));
        value.setEntity(entity);

        assertThat(assignedAndConsistentPredicate).accepts(value);
    }

    @Test
    void filtersForDeclarativeBasicEntities() {
        var solutionDescriptor = TestdataBasicVarSolution.buildSolutionDescriptor();
        var entityDescriptor = solutionDescriptor.getEntityDescriptorStrict(TestdataBasicVarEntity.class);
        // we don't care what variable descriptor, we just need one
        var variableDescriptor = entityDescriptor.getShadowVariableDescriptors().iterator().next();
        var entityForEachFilter = new EntityForEachFilter(entityDescriptor);

        var supplyManager = new ScoreDirectorIndependentSupplyManager();
        var assignedAndConsistentPredicate = entityForEachFilter.getAssignedAndConsistentPredicate(supplyManager);
        var consistentPredicate = entityForEachFilter.getConsistentPredicate(supplyManager);

        var entity = new TestdataBasicVarEntity("entity", null);
        var value = new TestdataBasicVarValue("value", Duration.ofHours(1L));
        var entityConsistencyState = supplyManager.demand(new EntityConsistencyStateDemand<>(entityDescriptor));

        entityConsistencyState.setEntityIsInconsistent(ChangedVariableNotifier.empty(), variableDescriptor, entity, false);
        assertThat(assignedAndConsistentPredicate).rejects(entity);
        assertThat(consistentPredicate).accepts(entity);

        entity.setValue(value);

        assertThat(assignedAndConsistentPredicate).accepts(entity);
        assertThat(consistentPredicate).accepts(entity);

        entityConsistencyState.setEntityIsInconsistent(ChangedVariableNotifier.empty(), variableDescriptor, entity, true);
        assertThat(assignedAndConsistentPredicate).rejects(entity);
        assertThat(consistentPredicate).rejects(entity);
    }

    @Test
    void filtersForDeclarativeListValueEntities() {
        var solutionDescriptor = TestdataConcurrentSolution.buildSolutionDescriptor();
        var entityDescriptor = solutionDescriptor.getEntityDescriptorStrict(TestdataConcurrentValue.class);
        var entityForEachFilter = new EntityForEachFilter(entityDescriptor);

        var supplyManager = new ScoreDirectorIndependentSupplyManager();
        var assignedAndConsistentPredicate = entityForEachFilter.getAssignedAndConsistentPredicate(supplyManager);
        var consistentPredicate = entityForEachFilter.getConsistentPredicate(supplyManager);

        var entity = new TestdataConcurrentEntity("entity");
        var value = new TestdataConcurrentValue("value");

        entity.setValues(List.of());
        value.setEntity(null);
        value.setInconsistent(false);

        assertThat(assignedAndConsistentPredicate).rejects(value);
        assertThat(consistentPredicate).accepts(value);

        entity.setValues(List.of(value));
        value.setEntity(entity);

        assertThat(assignedAndConsistentPredicate).accepts(value);
        assertThat(consistentPredicate).accepts(value);

        value.setInconsistent(true);

        assertThat(assignedAndConsistentPredicate).rejects(value);
        assertThat(consistentPredicate).rejects(value);
    }

    @Test
    void filtersForListValueEntitiesWithoutInverse() {
        var solutionDescriptor = TestdataPinnedNoShadowsListSolution.buildSolutionDescriptor();
        var entityDescriptor = solutionDescriptor.getEntityDescriptorStrict(TestdataPinnedNoShadowsListValue.class);
        var entityForEachFilter = new EntityForEachFilter(entityDescriptor);

        var supplyManager = new ScoreDirectorIndependentSupplyManager();
        var assignedAndConsistentPredicate = entityForEachFilter.getAssignedAndConsistentPredicate(supplyManager);
        var consistentPredicate = entityForEachFilter.getConsistentPredicate(supplyManager);

        assertThat(consistentPredicate).isNull();

        var entity = new TestdataPinnedNoShadowsListEntity("entity");
        var value = new TestdataPinnedNoShadowsListValue("value");

        entity.setValueList(List.of());

        assertThatCode(() -> assignedAndConsistentPredicate.test(value)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Impossible state");
    }

}