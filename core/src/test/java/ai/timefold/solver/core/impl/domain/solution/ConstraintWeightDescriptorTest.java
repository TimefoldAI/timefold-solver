package ai.timefold.solver.core.impl.domain.solution;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.impl.testdata.domain.constraintconfiguration.TestdataConstraintConfiguration;
import ai.timefold.solver.core.impl.testdata.domain.constraintconfiguration.TestdataConstraintConfigurationSolution;
import ai.timefold.solver.core.impl.testdata.domain.constraintconfiguration.extended.TestdataExtendedConstraintConfiguration;
import ai.timefold.solver.core.impl.testdata.domain.constraintconfiguration.extended.TestdataExtendedConstraintConfigurationSolution;

import org.junit.jupiter.api.Test;

class ConstraintWeightDescriptorTest {

    @Test
    void extractionFunction() {
        var solutionDescriptor = TestdataConstraintConfigurationSolution.buildSolutionDescriptor();
        var constraintConfigurationDescriptor =
                ((ConstraintConfigurationBasedConstraintWeightSupplier<SimpleScore, TestdataConstraintConfigurationSolution>) solutionDescriptor
                        .getConstraintWeightSupplier())
                        .getConstraintConfigurationDescriptor();

        var firstWeightDescriptor = constraintConfigurationDescriptor.getConstraintWeightDescriptor("firstWeight");
        assertThat(firstWeightDescriptor.getConstraintRef())
                .isEqualTo(
                        ConstraintRef.of(TestdataConstraintConfigurationSolution.class.getPackage().getName(), "First weight"));

        var secondWeightDescriptor = constraintConfigurationDescriptor.getConstraintWeightDescriptor("secondWeight");
        assertThat(secondWeightDescriptor.getConstraintRef())
                .isEqualTo(ConstraintRef.of("packageOverwrittenOnField", "Second weight"));

        var solution = new TestdataConstraintConfigurationSolution("solution");
        var constraintConfiguration = new TestdataConstraintConfiguration("constraintConfiguration");
        constraintConfiguration.setFirstWeight(SimpleScore.ZERO);
        constraintConfiguration.setSecondWeight(SimpleScore.of(7));
        solution.setConstraintConfiguration(constraintConfiguration);

        var accessor = solutionDescriptor.getConstraintConfigurationMemberAccessor();
        assertThat(accessor.executeGetter(solution)).isSameAs(constraintConfiguration);
        assertThat(firstWeightDescriptor.createExtractor(accessor).apply(solution)).isEqualTo(SimpleScore.ZERO);
        assertThat(secondWeightDescriptor.createExtractor(accessor).apply(solution)).isEqualTo(SimpleScore.of(7));
    }

    @Test
    void extractionFunctionExtended() {
        var solutionDescriptor = TestdataExtendedConstraintConfigurationSolution.buildExtendedSolutionDescriptor();
        var constraintConfigurationDescriptor =
                ((ConstraintConfigurationBasedConstraintWeightSupplier<SimpleScore, TestdataExtendedConstraintConfigurationSolution>) solutionDescriptor
                        .getConstraintWeightSupplier())
                        .getConstraintConfigurationDescriptor();

        var firstWeightDescriptor = constraintConfigurationDescriptor.getConstraintWeightDescriptor("firstWeight");
        assertThat(firstWeightDescriptor.getConstraintRef())
                .isEqualTo(
                        ConstraintRef.of(TestdataConstraintConfigurationSolution.class.getPackage().getName(), "First weight"));

        var secondWeightDescriptor = constraintConfigurationDescriptor.getConstraintWeightDescriptor("secondWeight");
        assertThat(secondWeightDescriptor.getConstraintRef())
                .isEqualTo(ConstraintRef.of("packageOverwrittenOnField", "Second weight"));

        var thirdWeightDescriptor = constraintConfigurationDescriptor.getConstraintWeightDescriptor("thirdWeight");
        assertThat(thirdWeightDescriptor.getConstraintRef())
                .isEqualTo(ConstraintRef.of(TestdataExtendedConstraintConfigurationSolution.class.getPackage().getName(),
                        "Third weight"));

        var solution = new TestdataExtendedConstraintConfigurationSolution("solution");
        var constraintConfiguration = new TestdataExtendedConstraintConfiguration("constraintConfiguration");
        constraintConfiguration.setFirstWeight(SimpleScore.ZERO);
        constraintConfiguration.setSecondWeight(SimpleScore.of(7));
        constraintConfiguration.setThirdWeight(SimpleScore.of(9));
        solution.setConstraintConfiguration(constraintConfiguration);

        var accessor = solutionDescriptor.getConstraintConfigurationMemberAccessor();
        assertThat(accessor.executeGetter(solution)).isSameAs(constraintConfiguration);
        assertThat(firstWeightDescriptor.createExtractor(accessor).apply(solution)).isEqualTo(SimpleScore.ZERO);
        assertThat(secondWeightDescriptor.createExtractor(accessor).apply(solution)).isEqualTo(SimpleScore.of(7));
        assertThat(thirdWeightDescriptor.createExtractor(accessor).apply(solution)).isEqualTo(SimpleScore.of(9));
    }

}
