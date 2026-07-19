package ai.timefold.solver.core.impl.domain.variable.declarative;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.move.test.MoveTester;
import ai.timefold.solver.core.testdomain.shadow.declarative.basicinverse.TestdataBasicInverseEntity;
import ai.timefold.solver.core.testdomain.shadow.declarative.basicinverse.TestdataBasicInverseGroup;
import ai.timefold.solver.core.testdomain.shadow.declarative.basicinverse.TestdataBasicInverseOwner;
import ai.timefold.solver.core.testdomain.shadow.declarative.basicinverse.TestdataBasicInverseSolution;

import org.junit.jupiter.api.Test;

/**
 * Covers the map-mode-only path of {@link ChangedVariableNotifier#getCollectionInverseVariableSupply}:
 * the "group" basic variable of {@link TestdataBasicInverseEntity} has no
 * {@code @InverseRelationShadowVariable} declared anywhere, so the declarative "ownerCode" shadow
 * (sourced from "group.owner") can only be kept correct if the framework demands a fresh,
 * non-externalized {@code BasicVariableStateSupply} to track "which entities point to a given group"
 * purely in memory.
 */
class BasicInverseShadowVariableTest {

    @Test
    void declarativeShadowTracksBasicVariableChangeThroughNonExternalizedInverseSupply() {
        var ownerX = new TestdataBasicInverseOwner("ownerX");
        var ownerY = new TestdataBasicInverseOwner("ownerY");

        var groupA = new TestdataBasicInverseGroup("groupA");
        groupA.setOwner(ownerX);
        var groupB = new TestdataBasicInverseGroup("groupB");
        groupB.setOwner(ownerY);

        var entity = new TestdataBasicInverseEntity("entity");
        entity.setGroup(groupA);

        var solution = new TestdataBasicInverseSolution(List.of(entity), List.of(groupA, groupB),
                List.of(ownerX, ownerY));

        var solutionMetaModel = PlanningSolutionMetaModel.of(TestdataBasicInverseSolution.class,
                TestdataBasicInverseEntity.class, TestdataBasicInverseGroup.class);
        var groupVariable = solutionMetaModel.genuineEntity(TestdataBasicInverseEntity.class)
                .basicVariable("group", TestdataBasicInverseGroup.class);
        var ownerVariable = solutionMetaModel.genuineEntity(TestdataBasicInverseGroup.class)
                .basicVariable("owner", TestdataBasicInverseOwner.class);

        var context = MoveTester.build(solutionMetaModel).using(solution);
        assertThat(entity.getOwnerCode()).isEqualTo("ownerX");

        // Real move: reassign the entity's basic variable ("group") to a different value.
        context.execute(Moves.change(groupVariable, entity, groupB));
        assertThat(entity.getGroup()).isSameAs(groupB);
        assertThat(entity.getOwnerCode()).isEqualTo("ownerY");

        // Real move: change the owner of the group the entity now points to. Since "group" has no
        // externalized inverse field, resolving "who currently points to groupB" for the declarative
        // recompute must go through the map-mode inverse supply.
        context.execute(Moves.change(ownerVariable, groupB, ownerX));
        assertThat(entity.getOwnerCode()).isEqualTo("ownerX");

        // Changing the owner of a group nobody points to must not affect the entity's shadow.
        context.execute(Moves.change(ownerVariable, groupA, ownerY));
        assertThat(entity.getOwnerCode()).isEqualTo("ownerX");
    }
}
