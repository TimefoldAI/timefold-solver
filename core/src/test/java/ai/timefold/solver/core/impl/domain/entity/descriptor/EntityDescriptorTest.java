package ai.timefold.solver.core.impl.domain.entity.descriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;

import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.immutable.TestdataSolution;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.pinned.TestdataExtendedPinnedEntity;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.pinned.TestdataExtendedPinnedSolution;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childtoo.TestdataBothAnnotatedChildEntity;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childtoo.TestdataBothAnnotatedExtendedSolution;
import ai.timefold.solver.core.testdomain.invalid.noplanningvar.TestdataNoVariableSolution;
import ai.timefold.solver.core.testdomain.pinned.TestdataPinnedEntity;

import org.junit.jupiter.api.Test;

class EntityDescriptorTest {

    @Test
    void movableEntitySelectionFilter() {
        var entityDescriptor = TestdataPinnedEntity.buildEntityDescriptor();
        assertThat(entityDescriptor.hasEffectiveMovableEntityFilter()).isTrue();
        var movableEntityFilter =
                entityDescriptor.getEffectiveMovableEntityFilter();
        assertThat(movableEntityFilter).isNotNull();

        assertThat(movableEntityFilter.test(null,
                new TestdataPinnedEntity("e1", null, false, false))).isTrue();
        assertThat(movableEntityFilter.test(null,
                new TestdataPinnedEntity("e2", null, true, false))).isFalse();
    }

    @Test
    void extendedMovableEntitySelectionFilterUsedByChildSelector() {
        var solutionDescriptor =
                TestdataExtendedPinnedSolution.buildSolutionDescriptor();

        var childEntityDescriptor =
                solutionDescriptor.findEntityDescriptor(TestdataExtendedPinnedEntity.class);
        assertThat(childEntityDescriptor.hasEffectiveMovableEntityFilter()).isTrue();
        var childMovableEntityFilter =
                childEntityDescriptor.getEffectiveMovableEntityFilter();
        assertThat(childMovableEntityFilter).isNotNull();

        // No new TestdataPinnedEntity() because a child selector would never select a pure parent instance
        assertThat(childMovableEntityFilter.test(null,
                new TestdataExtendedPinnedEntity("e3", null, false, false, null, false, false))).isTrue();
        assertThat(childMovableEntityFilter.test(null,
                new TestdataExtendedPinnedEntity("e4", null, true, false, null, false, false))).isFalse();
        assertThat(childMovableEntityFilter.test(null,
                new TestdataExtendedPinnedEntity("e5", null, false, true, null, false, false))).isFalse();
        assertThat(childMovableEntityFilter.test(null,
                new TestdataExtendedPinnedEntity("e6", null, false, false, null, true, false))).isFalse();
        assertThat(childMovableEntityFilter.test(null,
                new TestdataExtendedPinnedEntity("e7", null, false, false, null, false, true))).isFalse();
        assertThat(childMovableEntityFilter.test(null,
                new TestdataExtendedPinnedEntity("e8", null, true, true, null, true, true))).isFalse();
    }

    @Test
    void extractExtendedEntities() {
        var solution = new TestdataBothAnnotatedExtendedSolution("s1");

        var entity = new TestdataEntity("entity-singleton");
        solution.setEntity(entity);

        var subEntity = new TestdataBothAnnotatedChildEntity("subEntity-singleton");
        solution.setSubEntity(subEntity);

        var e1 = new TestdataEntity("entity1");
        var e2 = new TestdataEntity("entity2");
        solution.setEntityList(List.of(e1, e2));

        var s1 = new TestdataBothAnnotatedChildEntity("subEntity1");
        var s2 = new TestdataBothAnnotatedChildEntity("subEntity2");
        var s3 = new TestdataBothAnnotatedChildEntity("subEntity3");
        solution.setSubEntityList(List.of(s1, s2, s3));

        var r1 = new TestdataBothAnnotatedChildEntity("subEntity1-R");
        var r2 = new TestdataBothAnnotatedChildEntity("subEntity2-R");
        solution.setRawEntityList(List.of(r1, r2));

        var e3 = new TestdataEntity("entity3");
        var e4 = new TestdataEntity("entity4");
        var randomData = "randomData";
        solution.setObjectEntityList(List.of(e3, e4, randomData));

        var entityDescriptor =
                TestdataBothAnnotatedExtendedSolution.buildEntityDescriptor();
        assertThat(entityDescriptor.extractEntities(solution))
                .containsExactlyInAnyOrder(entity, subEntity, e1, e2, e3, e4, s1, s2, s3, r1, r2);
    }

    @Test
    void testImmutableClass() {
        assertThatCode(TestdataSolution::buildSolutionDescriptor)
                .hasMessageContaining("cannot be a record as it needs to be mutable.");
    }

    @Test
    void testEntityWithoutVariables() {
        assertThatCode(TestdataNoVariableSolution::buildSolutionDescriptor)
                .hasMessageContaining("must have at least 1 member with");
    }
}
