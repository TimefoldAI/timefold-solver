package ai.timefold.solver.migration;

import static org.openrewrite.java.Assertions.java;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

@Execution(ExecutionMode.CONCURRENT)
class GeneralChangeTypeMigrationRecipeTest implements RewriteTest {

    private static final class NoWildCardImportStyle extends NamedStyles {

        public NoWildCardImportStyle() {
            super(UUID.randomUUID(), "ImportStyle", "ImportStyle", "ImportStyle", Collections.emptySet(),
                    List.of(ImportLayoutStyle.builder().classCountToUseStarImport(9999999).importStaticAllOthers()
                            .importAllOthers().build()));
        }
    }

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(new GeneralChangeTypeMigrationRecipe())
                .typeValidationOptions(TypeValidation.builder().allowMissingType(ignore -> true).build())
                .parser(JavaParser.fromJavaVersion()
                        .styles(List.of(new NoWildCardImportStyle()))
                        // We must add all old classes as stubs to the JavaTemplate
                        .dependsOn("package ai.timefold.solver.core.api.domain.lookup; public class PlanningId {}",
                                "package ai.timefold.solver.core.api.score.buildin.simple; public class SimpleScore {}",
                                "package ai.timefold.solver.core.api.score.buildin.simplelong; public class SimpleLongScore {}",
                                "package ai.timefold.solver.core.api.score.buildin.simplebigdecimal; public class SimpleBigDecimalScore {}",
                                "package ai.timefold.solver.core.api.score.buildin.hardsoft; public class HardSoftScore {}",
                                "package ai.timefold.solver.core.api.score.buildin.hardsoftlong; public class HardSoftLongScore {}",
                                "package ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal; public class HardSoftBigDecimalScore {}",
                                "package ai.timefold.solver.core.api.score.buildin.hardmediumsoft; public class HardMediumSoftScore {}",
                                "package ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong; public class HardMediumSoftLongScore {}",
                                "package ai.timefold.solver.core.api.score.buildin.hardmediumsoftbigdecimal; public class HardMediumSoftBigDecimalScore {}",
                                "package ai.timefold.solver.core.api.score.buildin.bendable; public class BendableScore {}",
                                "package ai.timefold.solver.core.api.score.buildin.bendablelong; public class BendableLongScore {}",
                                "package ai.timefold.solver.core.api.score.buildin.bendablebigdecimal; public class BendableBigDecimalScore {}",
                                "package ai.timefold.solver.core.api.solver; public class ProblemFactChange {}",
                                "package ai.timefold.solver.core.config.solver; public enum EnvironmentMode {FAST_ASSERT, REPRODUCIBLE}"));
    }

    @Test
    void migrate() {
        rewriteRun(java(
                """
                        package timefold;

                        import ai.timefold.solver.core.api.domain.lookup.PlanningId;
                        import ai.timefold.solver.core.api.solver.ProblemFactChange;
                        import ai.timefold.solver.core.config.solver.EnvironmentMode;
                        import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
                        import ai.timefold.solver.core.api.score.buildin.simplelong.SimpleLongScore;
                        import ai.timefold.solver.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore;
                        import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
                        import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
                        import ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;
                        import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
                        import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
                        import ai.timefold.solver.core.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScore;
                        import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
                        import ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore;
                        import ai.timefold.solver.core.api.score.buildin.bendablebigdecimal.BendableBigDecimalScore;

                        public class Test {
                                @PlanningId
                                SimpleScore simpleScore;
                                ProblemFactChange  problemFactChange;
                                SimpleLongScore simpleLongScore;
                                SimpleBigDecimalScore simpleBigDecimalScore;
                                HardSoftScore hardSoftScore;
                                HardSoftLongScore hardSoftLongScore;
                                HardSoftBigDecimalScore hardSoftBigDecimalScore;
                                HardMediumSoftScore hardMediumSoftScore;
                                HardMediumSoftLongScore hardMediumSoftLongScore;
                                HardMediumSoftBigDecimalScore hardMediumSoftBigDecimalScore;
                                BendableScore bendableScore;
                                BendableLongScore bendableLongScore;
                                BendableBigDecimalScore bendableBigDecimalScore;
                                EnvironmentMode fast = EnvironmentMode.FAST_ASSERT;
                                EnvironmentMode reproducible = EnvironmentMode.REPRODUCIBLE;
                        }""",
                """
                        package timefold;
                        import ai.timefold.solver.core.api.solver.change.ProblemChange;
                        import ai.timefold.solver.core.config.solver.EnvironmentMode;
                        import ai.timefold.solver.core.api.domain.common.PlanningId;
                        import ai.timefold.solver.core.api.score.BendableBigDecimalScore;
                        import ai.timefold.solver.core.api.score.BendableScore;
                        import ai.timefold.solver.core.api.score.HardMediumSoftBigDecimalScore;
                        import ai.timefold.solver.core.api.score.HardMediumSoftScore;
                        import ai.timefold.solver.core.api.score.HardSoftBigDecimalScore;
                        import ai.timefold.solver.core.api.score.HardSoftScore;
                        import ai.timefold.solver.core.api.score.SimpleBigDecimalScore;
                        import ai.timefold.solver.core.api.score.SimpleScore;

                        public class Test {
                                @PlanningId
                                SimpleScore simpleScore;
                                ProblemChange  problemFactChange;
                                SimpleScore simpleLongScore;
                                SimpleBigDecimalScore simpleBigDecimalScore;
                                HardSoftScore hardSoftScore;
                                HardSoftScore hardSoftLongScore;
                                HardSoftBigDecimalScore hardSoftBigDecimalScore;
                                HardMediumSoftScore hardMediumSoftScore;
                                HardMediumSoftScore hardMediumSoftLongScore;
                                HardMediumSoftBigDecimalScore hardMediumSoftBigDecimalScore;
                                BendableScore bendableScore;
                                BendableScore bendableLongScore;
                                BendableBigDecimalScore bendableBigDecimalScore;
                                EnvironmentMode fast = EnvironmentMode.STEP_ASSERT;
                                EnvironmentMode reproducible = EnvironmentMode.NO_ASSERT;
                        }"""));
    }

}
