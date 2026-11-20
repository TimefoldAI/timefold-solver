package ai.timefold.solver.core.impl.solver;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.list.TestdataListInheritanceAnnotatedChildEntity;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.list.TestdataListInheritanceBaseEntity;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.list.TestdataListInheritanceSolution;

import org.junit.jupiter.api.Test;

/**
 * An entity class with a list variable must not be extended (user guide, "Inheritance for Planning Entities").
 */
class ListVariableEntityInheritanceTest {

    @Test
    void annotatedChildOfListVariableEntityIsRejected() {
        assertThatThrownBy(() -> SolutionDescriptor.buildSolutionDescriptor(TestdataListInheritanceSolution.class,
                TestdataListInheritanceBaseEntity.class, TestdataListInheritanceAnnotatedChildEntity.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContainingAll(TestdataListInheritanceAnnotatedChildEntity.class.getSimpleName(),
                        TestdataListInheritanceBaseEntity.class.getSimpleName(), "valueList", "@PlanningListVariable",
                        "must not be extended by another @PlanningEntity class", "Maybe keep a single entity class")
                .hasMessageNotContaining("multiple list variables");
    }

}
