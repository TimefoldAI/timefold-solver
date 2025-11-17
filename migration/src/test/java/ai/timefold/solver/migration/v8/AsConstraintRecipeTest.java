package ai.timefold.solver.migration.v8;

import static org.openrewrite.java.Assertions.java;

import ai.timefold.solver.migration.AbstractRecipe;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

@Execution(ExecutionMode.CONCURRENT)
class AsConstraintRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AsConstraintRecipe())
                .parser(AbstractRecipe.JAVA_PARSER);
    }

    // ************************************************************************
    // Uni
    // ************************************************************************

    @Test
    void uniPenalizeName() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .penalize("My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .penalize(HardSoftScore.ONE_HARD)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniPenalizeId() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .penalize("My package", "My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .penalize(HardSoftScore.ONE_HARD)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniPenalizeConfigurableName() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .penalizeConfigurable("My constraint");\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .penalizeConfigurable()
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniPenalizeConfigurableId() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .penalizeConfigurable("My package", "My constraint");\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .penalizeConfigurable()
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniPenalizeNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .penalize("My constraint", HardSoftScore.ONE_HARD, (a) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .penalize(HardSoftScore.ONE_HARD, (a) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniPenalizeIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .penalize("My package", "My constraint", HardSoftScore.ONE_HARD, (a) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .penalize(HardSoftScore.ONE_HARD, (a) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniPenalizeConfigurableNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .penalizeConfigurable("My constraint", (a) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .penalizeConfigurable((a) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniPenalizeConfigurableIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .penalizeConfigurable("My package", "My constraint", (a) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .penalizeConfigurable((a) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniPenalizeNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .penalizeLong("My constraint", HardSoftLongScore.ONE_HARD, (a) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .penalizeLong(HardSoftLongScore.ONE_HARD, (a) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniPenalizeIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .penalizeLong("My package", "My constraint", HardSoftLongScore.ONE_HARD, (a) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .penalizeLong(HardSoftLongScore.ONE_HARD, (a) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniPenalizeConfigurableNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .penalizeConfigurableLong("My constraint", (a) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .penalizeConfigurableLong((a) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniPenalizeConfigurableIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .penalizeConfigurableLong("My package", "My constraint", (a) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .penalizeConfigurableLong((a) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniPenalizeNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .penalizeBigDecimal("My constraint", HardSoftBigDecimalScore.ONE_HARD, (a) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .penalizeBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniPenalizeIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .penalizeBigDecimal("My package", "My constraint", HardSoftBigDecimalScore.ONE_HARD, (a) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .penalizeBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniPenalizeConfigurableNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .penalizeConfigurableBigDecimal("My constraint", (a) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .penalizeConfigurableBigDecimal((a) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniPenalizeConfigurableIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .penalizeConfigurableBigDecimal("My package", "My constraint", (a) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .penalizeConfigurableBigDecimal((a) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniRewardName() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .reward("My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .reward(HardSoftScore.ONE_HARD)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniRewardId() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .reward("My package", "My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .reward(HardSoftScore.ONE_HARD)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniRewardConfigurableName() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .rewardConfigurable("My constraint");\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .rewardConfigurable()
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniRewardConfigurableId() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .rewardConfigurable("My package", "My constraint");\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .rewardConfigurable()
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniRewardNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .reward("My constraint", HardSoftScore.ONE_HARD, (a) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .reward(HardSoftScore.ONE_HARD, (a) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniRewardIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .reward("My package", "My constraint", HardSoftScore.ONE_HARD, (a) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .reward(HardSoftScore.ONE_HARD, (a) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniRewardConfigurableNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .rewardConfigurable("My constraint", (a) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .rewardConfigurable((a) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniRewardConfigurableIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .rewardConfigurable("My package", "My constraint", (a) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .rewardConfigurable((a) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniRewardNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .rewardLong("My constraint", HardSoftLongScore.ONE_HARD, (a) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .rewardLong(HardSoftLongScore.ONE_HARD, (a) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniRewardIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .rewardLong("My package", "My constraint", HardSoftLongScore.ONE_HARD, (a) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .rewardLong(HardSoftLongScore.ONE_HARD, (a) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniRewardConfigurableNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .rewardConfigurableLong("My constraint", (a) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .rewardConfigurableLong((a) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniRewardConfigurableIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .rewardConfigurableLong("My package", "My constraint", (a) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .rewardConfigurableLong((a) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniRewardNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .rewardBigDecimal("My constraint", HardSoftBigDecimalScore.ONE_HARD, (a) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .rewardBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniRewardIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .rewardBigDecimal("My package", "My constraint", HardSoftBigDecimalScore.ONE_HARD, (a) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .rewardBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniRewardConfigurableNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .rewardConfigurableBigDecimal("My constraint", (a) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .rewardConfigurableBigDecimal((a) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniRewardConfigurableIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .rewardConfigurableBigDecimal("My package", "My constraint", (a) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .rewardConfigurableBigDecimal((a) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniImpactName() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .impact("My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .impact(HardSoftScore.ONE_HARD)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniImpactId() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .impact("My package", "My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .impact(HardSoftScore.ONE_HARD)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniImpactNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .impact("My constraint", HardSoftScore.ONE_HARD, (a) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .impact(HardSoftScore.ONE_HARD, (a) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniImpactIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .impact("My package", "My constraint", HardSoftScore.ONE_HARD, (a) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .impact(HardSoftScore.ONE_HARD, (a) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniImpactNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .impactLong("My constraint", HardSoftLongScore.ONE_HARD, (a) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .impactLong(HardSoftLongScore.ONE_HARD, (a) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniImpactIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .impactLong("My package", "My constraint", HardSoftLongScore.ONE_HARD, (a) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .impactLong(HardSoftLongScore.ONE_HARD, (a) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniImpactNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .impactBigDecimal("My constraint", HardSoftBigDecimalScore.ONE_HARD, (a) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .impactBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniImpactIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .impactBigDecimal("My package", "My constraint", HardSoftBigDecimalScore.ONE_HARD, (a) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .impactBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    // ************************************************************************
    // Bi
    // ************************************************************************

    @Test
    void biPenalizeName() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .penalize("My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .penalize(HardSoftScore.ONE_HARD)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biPenalizeId() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .penalize("My package", "My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .penalize(HardSoftScore.ONE_HARD)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biPenalizeConfigurableName() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable("My constraint");\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable()
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biPenalizeConfigurableId() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable("My package", "My constraint");\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable()
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biPenalizeNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .penalize("My constraint", HardSoftScore.ONE_HARD, (a, b) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .penalize(HardSoftScore.ONE_HARD, (a, b) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biPenalizeIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .penalize("My package", "My constraint", HardSoftScore.ONE_HARD, (a, b) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .penalize(HardSoftScore.ONE_HARD, (a, b) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biPenalizeConfigurableNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable("My constraint", (a, b) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable((a, b) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biPenalizeConfigurableIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable("My package", "My constraint", (a, b) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable((a, b) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biPenalizeNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeLong("My constraint", HardSoftLongScore.ONE_HARD, (a, b) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeLong(HardSoftLongScore.ONE_HARD, (a, b) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biPenalizeIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeLong("My package", "My constraint", HardSoftLongScore.ONE_HARD, (a, b) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeLong(HardSoftLongScore.ONE_HARD, (a, b) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biPenalizeConfigurableNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableLong("My constraint", (a, b) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableLong((a, b) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biPenalizeConfigurableIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableLong("My package", "My constraint", (a, b) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableLong((a, b) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biPenalizeNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeBigDecimal("My constraint", HardSoftBigDecimalScore.ONE_HARD, (a, b) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a, b) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biPenalizeIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeBigDecimal("My package", "My constraint", HardSoftBigDecimalScore.ONE_HARD, (a, b) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a, b) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biPenalizeConfigurableNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableBigDecimal("My constraint", (a, b) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableBigDecimal((a, b) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biPenalizeConfigurableIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableBigDecimal("My package", "My constraint", (a, b) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableBigDecimal((a, b) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biRewardName() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .reward("My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .reward(HardSoftScore.ONE_HARD)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biRewardId() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .reward("My package", "My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .reward(HardSoftScore.ONE_HARD)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biRewardConfigurableName() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .rewardConfigurable("My constraint");\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .rewardConfigurable()
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biRewardConfigurableId() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .rewardConfigurable("My package", "My constraint");\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .rewardConfigurable()
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biRewardNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .reward("My constraint", HardSoftScore.ONE_HARD, (a, b) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .reward(HardSoftScore.ONE_HARD, (a, b) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biRewardIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .reward("My package", "My constraint", HardSoftScore.ONE_HARD, (a, b) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .reward(HardSoftScore.ONE_HARD, (a, b) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biRewardConfigurableNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .rewardConfigurable("My constraint", (a, b) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .rewardConfigurable((a, b) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biRewardConfigurableIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .rewardConfigurable("My package", "My constraint", (a, b) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .rewardConfigurable((a, b) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biRewardNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .rewardLong("My constraint", HardSoftLongScore.ONE_HARD, (a, b) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .rewardLong(HardSoftLongScore.ONE_HARD, (a, b) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biRewardIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .rewardLong("My package", "My constraint", HardSoftLongScore.ONE_HARD, (a, b) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .rewardLong(HardSoftLongScore.ONE_HARD, (a, b) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biRewardConfigurableNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .rewardConfigurableLong("My constraint", (a, b) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .rewardConfigurableLong((a, b) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biRewardConfigurableIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .rewardConfigurableLong("My package", "My constraint", (a, b) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .rewardConfigurableLong((a, b) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biRewardNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .rewardBigDecimal("My constraint", HardSoftBigDecimalScore.ONE_HARD, (a, b) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .rewardBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a, b) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biRewardIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .rewardBigDecimal("My package", "My constraint", HardSoftBigDecimalScore.ONE_HARD, (a, b) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .rewardBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a, b) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biRewardConfigurableNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .rewardConfigurableBigDecimal("My constraint", (a, b) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .rewardConfigurableBigDecimal((a, b) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biRewardConfigurableIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .rewardConfigurableBigDecimal("My package", "My constraint", (a, b) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .rewardConfigurableBigDecimal((a, b) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biImpactName() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .impact("My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .impact(HardSoftScore.ONE_HARD)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biImpactId() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .impact("My package", "My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .impact(HardSoftScore.ONE_HARD)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biImpactNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .impact("My constraint", HardSoftScore.ONE_HARD, (a, b) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .impact(HardSoftScore.ONE_HARD, (a, b) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biImpactIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .impact("My package", "My constraint", HardSoftScore.ONE_HARD, (a, b) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .impact(HardSoftScore.ONE_HARD, (a, b) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biImpactNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .impactLong("My constraint", HardSoftLongScore.ONE_HARD, (a, b) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .impactLong(HardSoftLongScore.ONE_HARD, (a, b) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biImpactIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .impactLong("My package", "My constraint", HardSoftLongScore.ONE_HARD, (a, b) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .impactLong(HardSoftLongScore.ONE_HARD, (a, b) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biImpactNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .impactBigDecimal("My constraint", HardSoftBigDecimalScore.ONE_HARD, (a, b) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .impactBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a, b) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biImpactIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .impactBigDecimal("My package", "My constraint", HardSoftBigDecimalScore.ONE_HARD, (a, b) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .impactBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a, b) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    // ************************************************************************
    // Tri
    // ************************************************************************

    @Test
    void triPenalizeName() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalize("My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalize(HardSoftScore.ONE_HARD)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triPenalizeId() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalize("My package", "My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalize(HardSoftScore.ONE_HARD)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triPenalizeConfigurableName() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable("My constraint");\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable()
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triPenalizeConfigurableId() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable("My package", "My constraint");\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable()
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triPenalizeNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalize("My constraint", HardSoftScore.ONE_HARD, (a, b, c) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalize(HardSoftScore.ONE_HARD, (a, b, c) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triPenalizeIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalize("My package", "My constraint", HardSoftScore.ONE_HARD, (a, b, c) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalize(HardSoftScore.ONE_HARD, (a, b, c) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triPenalizeConfigurableNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable("My constraint", (a, b, c) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable((a, b, c) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triPenalizeConfigurableIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable("My package", "My constraint", (a, b, c) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable((a, b, c) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triPenalizeNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeLong("My constraint", HardSoftLongScore.ONE_HARD, (a, b, c) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeLong(HardSoftLongScore.ONE_HARD, (a, b, c) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triPenalizeIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeLong("My package", "My constraint", HardSoftLongScore.ONE_HARD, (a, b, c) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeLong(HardSoftLongScore.ONE_HARD, (a, b, c) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triPenalizeConfigurableNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableLong("My constraint", (a, b, c) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableLong((a, b, c) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triPenalizeConfigurableIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableLong("My package", "My constraint", (a, b, c) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableLong((a, b, c) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triPenalizeNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeBigDecimal("My constraint", HardSoftBigDecimalScore.ONE_HARD, (a, b, c) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a, b, c) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triPenalizeIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeBigDecimal("My package", "My constraint", HardSoftBigDecimalScore.ONE_HARD, (a, b, c) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a, b, c) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triPenalizeConfigurableNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableBigDecimal("My constraint", (a, b, c) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableBigDecimal((a, b, c) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triPenalizeConfigurableIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableBigDecimal("My package", "My constraint", (a, b, c) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableBigDecimal((a, b, c) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triRewardName() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .reward("My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .reward(HardSoftScore.ONE_HARD)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triRewardId() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .reward("My package", "My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .reward(HardSoftScore.ONE_HARD)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triRewardConfigurableName() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurable("My constraint");\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurable()
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triRewardConfigurableId() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurable("My package", "My constraint");\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurable()
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triRewardNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .reward("My constraint", HardSoftScore.ONE_HARD, (a, b, c) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .reward(HardSoftScore.ONE_HARD, (a, b, c) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triRewardIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .reward("My package", "My constraint", HardSoftScore.ONE_HARD, (a, b, c) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .reward(HardSoftScore.ONE_HARD, (a, b, c) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triRewardConfigurableNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurable("My constraint", (a, b, c) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurable((a, b, c) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triRewardConfigurableIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurable("My package", "My constraint", (a, b, c) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurable((a, b, c) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triRewardNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardLong("My constraint", HardSoftLongScore.ONE_HARD, (a, b, c) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardLong(HardSoftLongScore.ONE_HARD, (a, b, c) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triRewardIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardLong("My package", "My constraint", HardSoftLongScore.ONE_HARD, (a, b, c) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardLong(HardSoftLongScore.ONE_HARD, (a, b, c) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triRewardConfigurableNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurableLong("My constraint", (a, b, c) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurableLong((a, b, c) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triRewardConfigurableIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurableLong("My package", "My constraint", (a, b, c) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurableLong((a, b, c) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triRewardNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardBigDecimal("My constraint", HardSoftBigDecimalScore.ONE_HARD, (a, b, c) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a, b, c) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triRewardIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardBigDecimal("My package", "My constraint", HardSoftBigDecimalScore.ONE_HARD, (a, b, c) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a, b, c) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triRewardConfigurableNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurableBigDecimal("My constraint", (a, b, c) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurableBigDecimal((a, b, c) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triRewardConfigurableIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurableBigDecimal("My package", "My constraint", (a, b, c) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurableBigDecimal((a, b, c) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triImpactName() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impact("My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impact(HardSoftScore.ONE_HARD)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triImpactId() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impact("My package", "My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impact(HardSoftScore.ONE_HARD)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triImpactNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impact("My constraint", HardSoftScore.ONE_HARD, (a, b, c) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impact(HardSoftScore.ONE_HARD, (a, b, c) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triImpactIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impact("My package", "My constraint", HardSoftScore.ONE_HARD, (a, b, c) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impact(HardSoftScore.ONE_HARD, (a, b, c) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triImpactNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impactLong("My constraint", HardSoftLongScore.ONE_HARD, (a, b, c) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impactLong(HardSoftLongScore.ONE_HARD, (a, b, c) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triImpactIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impactLong("My package", "My constraint", HardSoftLongScore.ONE_HARD, (a, b, c) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impactLong(HardSoftLongScore.ONE_HARD, (a, b, c) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triImpactNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impactBigDecimal("My constraint", HardSoftBigDecimalScore.ONE_HARD, (a, b, c) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impactBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a, b, c) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triImpactIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impactBigDecimal("My package", "My constraint", HardSoftBigDecimalScore.ONE_HARD, (a, b, c) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impactBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a, b, c) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    // ************************************************************************
    // Quad
    // ************************************************************************

    @Test
    void quadPenalizeName() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalize("My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalize(HardSoftScore.ONE_HARD)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadPenalizeId() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalize("My package", "My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalize(HardSoftScore.ONE_HARD)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadPenalizeConfigurableName() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable("My constraint");\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable()
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadPenalizeConfigurableId() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable("My package", "My constraint");\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable()
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadPenalizeNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalize("My constraint", HardSoftScore.ONE_HARD, (a, b, c, d) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalize(HardSoftScore.ONE_HARD, (a, b, c, d) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadPenalizeIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalize("My package", "My constraint", HardSoftScore.ONE_HARD, (a, b, c, d) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalize(HardSoftScore.ONE_HARD, (a, b, c, d) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadPenalizeConfigurableNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable("My constraint", (a, b, c, d) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable((a, b, c, d) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadPenalizeConfigurableIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable("My package", "My constraint", (a, b, c, d) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable((a, b, c, d) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadPenalizeNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeLong("My constraint", HardSoftLongScore.ONE_HARD, (a, b, c, d) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeLong(HardSoftLongScore.ONE_HARD, (a, b, c, d) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadPenalizeIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeLong("My package", "My constraint", HardSoftLongScore.ONE_HARD, (a, b, c, d) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeLong(HardSoftLongScore.ONE_HARD, (a, b, c, d) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadPenalizeConfigurableNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableLong("My constraint", (a, b, c, d) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableLong((a, b, c, d) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadPenalizeConfigurableIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableLong("My package", "My constraint", (a, b, c, d) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableLong((a, b, c, d) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadPenalizeNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeBigDecimal("My constraint", HardSoftBigDecimalScore.ONE_HARD, (a, b, c, d) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a, b, c, d) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadPenalizeIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeBigDecimal("My package", "My constraint", HardSoftBigDecimalScore.ONE_HARD, (a, b, c, d) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a, b, c, d) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadPenalizeConfigurableNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableBigDecimal("My constraint", (a, b, c, d) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableBigDecimal((a, b, c, d) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadPenalizeConfigurableIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableBigDecimal("My package", "My constraint", (a, b, c, d) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableBigDecimal((a, b, c, d) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadRewardName() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .reward("My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .reward(HardSoftScore.ONE_HARD)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadRewardId() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .reward("My package", "My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .reward(HardSoftScore.ONE_HARD)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadRewardConfigurableName() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurable("My constraint");\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurable()
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadRewardConfigurableId() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurable("My package", "My constraint");\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurable()
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadRewardNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .reward("My constraint", HardSoftScore.ONE_HARD, (a, b, c, d) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .reward(HardSoftScore.ONE_HARD, (a, b, c, d) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadRewardIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .reward("My package", "My constraint", HardSoftScore.ONE_HARD, (a, b, c, d) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .reward(HardSoftScore.ONE_HARD, (a, b, c, d) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadRewardConfigurableNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurable("My constraint", (a, b, c, d) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurable((a, b, c, d) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadRewardConfigurableIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurable("My package", "My constraint", (a, b, c, d) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurable((a, b, c, d) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadRewardNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardLong("My constraint", HardSoftLongScore.ONE_HARD, (a, b, c, d) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardLong(HardSoftLongScore.ONE_HARD, (a, b, c, d) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadRewardIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardLong("My package", "My constraint", HardSoftLongScore.ONE_HARD, (a, b, c, d) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardLong(HardSoftLongScore.ONE_HARD, (a, b, c, d) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadRewardConfigurableNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurableLong("My constraint", (a, b, c, d) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurableLong((a, b, c, d) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadRewardConfigurableIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurableLong("My package", "My constraint", (a, b, c, d) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurableLong((a, b, c, d) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadRewardNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardBigDecimal("My constraint", HardSoftBigDecimalScore.ONE_HARD, (a, b, c, d) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a, b, c, d) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadRewardIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardBigDecimal("My package", "My constraint", HardSoftBigDecimalScore.ONE_HARD, (a, b, c, d) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a, b, c, d) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadRewardConfigurableNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurableBigDecimal("My constraint", (a, b, c, d) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurableBigDecimal((a, b, c, d) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadRewardConfigurableIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurableBigDecimal("My package", "My constraint", (a, b, c, d) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurableBigDecimal((a, b, c, d) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadImpactName() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impact("My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impact(HardSoftScore.ONE_HARD)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadImpactId() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impact("My package", "My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impact(HardSoftScore.ONE_HARD)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadImpactNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impact("My constraint", HardSoftScore.ONE_HARD, (a, b, c, d) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impact(HardSoftScore.ONE_HARD, (a, b, c, d) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadImpactIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impact("My package", "My constraint", HardSoftScore.ONE_HARD, (a, b, c, d) -> 7);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impact(HardSoftScore.ONE_HARD, (a, b, c, d) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadImpactNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impactLong("My constraint", HardSoftLongScore.ONE_HARD, (a, b, c, d) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impactLong(HardSoftLongScore.ONE_HARD, (a, b, c, d) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadImpactIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impactLong("My package", "My constraint", HardSoftLongScore.ONE_HARD, (a, b, c, d) -> 7L);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impactLong(HardSoftLongScore.ONE_HARD, (a, b, c, d) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadImpactNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impactBigDecimal("My constraint", HardSoftBigDecimalScore.ONE_HARD, (a, b, c, d) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impactBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a, b, c, d) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadImpactIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impactBigDecimal("My package", "My constraint", HardSoftBigDecimalScore.ONE_HARD, (a, b, c, d) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                return f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impactBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a, b, c, d) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    // ************************************************************************
    // Helper methods
    // ************************************************************************

    private static @Language("java") String wrap(@Language("java") String content) {
        return "import java.math.BigDecimal;\n" +
                "import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;\n" +
                "import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;\n" +
                "import ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;\n" +
                "import ai.timefold.solver.core.api.score.stream.ConstraintFactory;\n" +
                "import ai.timefold.solver.core.api.score.stream.Constraint;\n" +
                "\n" +
                "class Test {\n" +
                "    Constraint myConstraint(ConstraintFactory f) {\n" +
                content + "\n" +
                "    }" +
                "}\n";
    }

}
