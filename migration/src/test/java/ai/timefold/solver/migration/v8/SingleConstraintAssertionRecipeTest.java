package ai.timefold.solver.migration.v8;

import static org.openrewrite.java.Assertions.java;

import ai.timefold.solver.migration.AbstractRecipe;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

class SingleConstraintAssertionRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new SingleConstraintAssertionMethodsRecipe())
                .parser(AbstractRecipe.JAVA_PARSER);
    }

    @Test
    void constraintMethods() {
        runTest(
                """
                        constraintVerifier.verifyThat((constraintProvider, constraintFactory) -> null).given(null).penalizesBy(1, "test");
                        constraintVerifier.verifyThat((constraintProvider, constraintFactory) -> null).given(null).rewardsWith(1, "test");
                        constraintVerifier.verifyThat((constraintProvider, constraintFactory) -> null).given(null).penalizesBy(1L, "test");
                        constraintVerifier.verifyThat((constraintProvider, constraintFactory) -> null).given(null).penalizes(1L, "test");
                        constraintVerifier.verifyThat((constraintProvider, constraintFactory) -> null).given(null).rewardsWith(1L, "test");
                        constraintVerifier.verifyThat((constraintProvider, constraintFactory) -> null).given(null).rewards(1L, "test");
                        constraintVerifier.verifyThat((constraintProvider, constraintFactory) -> null).given(null).penalizesBy(BigDecimal.ONE, "test");
                        constraintVerifier.verifyThat((constraintProvider, constraintFactory) -> null).given(null).rewardsWith(BigDecimal.ONE, "test");""",
                """
                        constraintVerifier.verifyThat((constraintProvider, constraintFactory) -> null).given(null).penalizesBy("test", 1);
                        constraintVerifier.verifyThat((constraintProvider, constraintFactory) -> null).given(null).rewardsWith("test", 1);
                        constraintVerifier.verifyThat((constraintProvider, constraintFactory) -> null).given(null).penalizesBy("test", 1L);
                        constraintVerifier.verifyThat((constraintProvider, constraintFactory) -> null).given(null).penalizes("test", 1L);
                        constraintVerifier.verifyThat((constraintProvider, constraintFactory) -> null).given(null).rewardsWith("test", 1L);
                        constraintVerifier.verifyThat((constraintProvider, constraintFactory) -> null).given(null).rewards("test", 1L);
                        constraintVerifier.verifyThat((constraintProvider, constraintFactory) -> null).given(null).penalizesBy("test", BigDecimal.ONE);
                        constraintVerifier.verifyThat((constraintProvider, constraintFactory) -> null).given(null).rewardsWith("test", BigDecimal.ONE);""");
    }

    private void runTest(String before, String after) {
        rewriteRun(java(wrap(before), wrap(after)));
    }

    private static String wrap(String content) {
        return """
                import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;
                import java.math.BigDecimal;

                public class Test {

                    public void validate(ConstraintVerifier<?, ?> constraintVerifier) {
                    %8s%s
                    }
                }"""
                .formatted("", content);

    }

}
