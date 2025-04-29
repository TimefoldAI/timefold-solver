package ai.timefold.solver.core.impl.score.director.stream;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.List;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.testconstraint.TestConstraint;
import ai.timefold.solver.core.testconstraint.TestConstraintFactory;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class DefaultConstraintMetaModelTest {

    @Test
    void test() {
        var solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        var constraintFactory = new TestConstraintFactory<TestdataSolution, SimpleScore>(solutionDescriptor);
        var constraint1 = new TestConstraint<>(constraintFactory, "Test Constraint 1", SimpleScore.of(1));
        var constraint2 = new TestConstraint<>(constraintFactory, "Test Constraint 2", SimpleScore.of(10));
        var constraint3 = new TestConstraint<>(constraintFactory, "Test Constraint 3", "test", SimpleScore.of(100));
        var constraint4 = new TestConstraint<>(constraintFactory, "Test Constraint 4", "another-test", SimpleScore.ZERO);
        var metaModel = DefaultConstraintMetaModel.of(List.of(constraint1, constraint2, constraint3, constraint4));

        assertSoftly(softly -> {
            softly.assertThat(metaModel.getConstraint(constraint1.getConstraintRef())).isSameAs(constraint1);
            softly.assertThat(metaModel.getConstraint(constraint2.getConstraintRef())).isSameAs(constraint2);
            softly.assertThat(metaModel.getConstraint(constraint3.getConstraintRef())).isSameAs(constraint3);
            softly.assertThat(metaModel.getConstraint(constraint4.getConstraintRef())).isSameAs(constraint4);
        });

        Assertions.assertThat(metaModel.getConstraintGroups())
                .containsExactly("another-test", Constraint.DEFAULT_CONSTRAINT_GROUP, "test");

        assertSoftly(softly -> {
            softly.assertThat(metaModel.getConstraintsPerGroup(Constraint.DEFAULT_CONSTRAINT_GROUP))
                    .containsExactly(constraint1, constraint2);
            softly.assertThat(metaModel.getConstraintsPerGroup("test")).containsExactly(constraint3);
            softly.assertThat(metaModel.getConstraintsPerGroup("another-test")).containsExactly(constraint4);
        });
    }

}
