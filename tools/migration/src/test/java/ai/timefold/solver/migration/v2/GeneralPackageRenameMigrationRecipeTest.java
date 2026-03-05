package ai.timefold.solver.migration.v2;

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
class GeneralPackageRenameMigrationRecipeTest implements RewriteTest {

    private static final class NoWildCardImportStyle extends NamedStyles {

        public NoWildCardImportStyle() {
            super(UUID.randomUUID(), "ImportStyle", "ImportStyle", "ImportStyle", Collections.emptySet(),
                    List.of(ImportLayoutStyle.builder().classCountToUseStarImport(9999999).importStaticAllOthers()
                            .importAllOthers().build()));
        }
    }

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(new GeneralPackageRenameMigrationRecipe())
                .typeValidationOptions(TypeValidation.builder().allowMissingType(ignore -> true).build())
                .parser(JavaParser.fromJavaVersion().styles(List.of(new NoWildCardImportStyle()))
                        // We must add all old classes as stubs to the JavaTemplate
                        .dependsOn(
                                // Persistence API
                                "package ai.timefold.solver.persistence.common.api.domain.solution; public class SolutionFileIO {}",
                                "package ai.timefold.solver.jpa.api.score.buildin.bendablebigdecimal; public class BendableBigDecimalScoreConverter {}",
                                "package ai.timefold.solver.jpa.api.score.buildin.bendable; public class BendableScoreConverter {}",
                                "package ai.timefold.solver.jpa.api.score.buildin.hardmediumsoftbigdecimal; public class HardMediumSoftBigDecimalScoreConverter {}",
                                "package ai.timefold.solver.jpa.api.score.buildin.hardmediumsoft; public class HardMediumSoftScoreConverter {}",
                                "package ai.timefold.solver.jpa.api.score.buildin.hardsoftbigdecimal; public class HardSoftBigDecimalScoreConverter {}",
                                "package ai.timefold.solver.jpa.api.score.buildin.hardsoft; public class HardSoftScoreConverter {}",
                                "package ai.timefold.solver.jpa.api.score.buildin.simplebigdecimal; public class SimpleBigDecimalScoreConverter {}",
                                "package ai.timefold.solver.jpa.api.score.buildin.simple; public class SimpleScoreConverter {}",
                                // Jackson API
                                "package ai.timefold.solver.jackson.api.score.buildin.bendablebigdecimal; public class BendableBigDecimalScoreJacksonDeserializer {}",
                                "package ai.timefold.solver.jackson.api.score.buildin.bendable; public class BendableScoreJacksonDeserializer {}",
                                "package ai.timefold.solver.jackson.api.score.buildin.hardmediumsoftbigdecimal; public class HardMediumSoftBigDecimalScoreJacksonDeserializer {}",
                                "package ai.timefold.solver.jackson.api.score.buildin.hardmediumsoft; public class HardMediumSoftScoreJacksonDeserializer {}",
                                "package ai.timefold.solver.jackson.api.score.buildin.hardsoftbigdecimal; public class HardSoftBigDecimalScoreJacksonDeserializer {}",
                                "package ai.timefold.solver.jackson.api.score.buildin.hardsoft; public class HardSoftScoreJacksonDeserializer {}",
                                "package ai.timefold.solver.jackson.api.score.buildin.simplebigdecimal; public class SimpleBigDecimalScoreJacksonDeserializer {}",
                                "package ai.timefold.solver.jackson.api.score.buildin.simple; public class SimpleScoreJacksonDeserializer {}",
                                // JAXB API
                                "package ai.timefold.solver.jaxb.api.score.buildin.bendablebigdecimal; public class BendableBigDecimalScoreJaxbAdapter {}",
                                "package ai.timefold.solver.jaxb.api.score.buildin.bendable; public class BendableScoreJaxbAdapter {}",
                                "package ai.timefold.solver.jaxb.api.score.buildin.hardmediumsoftbigdecimal; public class HardMediumSoftBigDecimalScoreJaxbAdapter {}",
                                "package ai.timefold.solver.jaxb.api.score.buildin.hardmediumsoft; public class HardMediumSoftScoreJaxbAdapter {}",
                                "package ai.timefold.solver.jaxb.api.score.buildin.hardsoftbigdecimal; public class HardSoftBigDecimalScoreJaxbAdapter {}",
                                "package ai.timefold.solver.jaxb.api.score.buildin.hardsoft; public class HardSoftScoreJaxbAdapter {}",
                                "package ai.timefold.solver.jaxb.api.score.buildin.simplebigdecimal; public class SimpleBigDecimalScoreJaxbAdapter {}",
                                "package ai.timefold.solver.jaxb.api.score.buildin.simple; public class SimpleScoreJaxbAdapter {}",
                                // Jackson API
                                "package ai.timefold.solver.quarkus.jackson.score.buildin.bendablebigdecimal; public class BendableBigDecimalScoreJacksonDeserializer {}",
                                "package ai.timefold.solver.quarkus.jackson.score.buildin.bendable; public class BendableScoreJacksonDeserializer {}",
                                "package ai.timefold.solver.quarkus.jackson.score.buildin.hardmediumsoftbigdecimal; public class HardMediumSoftBigDecimalScoreJacksonDeserializer {}",
                                "package ai.timefold.solver.quarkus.jackson.score.buildin.hardmediumsoft; public class HardMediumSoftScoreJacksonDeserializer {}",
                                "package ai.timefold.solver.quarkus.jackson.score.buildin.hardsoftbigdecimal; public class HardSoftBigDecimalScoreJacksonDeserializer {}",
                                "package ai.timefold.solver.quarkus.jackson.score.buildin.hardsoft; public class HardSoftScoreJacksonDeserializer {}",
                                "package ai.timefold.solver.quarkus.jackson.score.buildin.simplebigdecimal; public class SimpleBigDecimalScoreJacksonDeserializer {}",
                                "package ai.timefold.solver.quarkus.jackson.score.buildin.simple; public class SimpleScoreJacksonDeserializer {}",
                                // Value Range API
                                "package ai.timefold.solver.core.impl.domain.valuerange.buildin.bigdecimal; public class BigDecimalValueRange {}",
                                "package ai.timefold.solver.core.impl.domain.valuerange.buildin.biginteger; public class BigIntegerValueRange {}",
                                "package ai.timefold.solver.core.impl.domain.valuerange.buildin.primboolean; public class BooleanValueRange {}",
                                "package ai.timefold.solver.core.impl.domain.valuerange.buildin.primint; public class IntValueRange {}",
                                "package ai.timefold.solver.core.impl.domain.valuerange.buildin.collection; public class ListValueRange {}",
                                "package ai.timefold.solver.core.impl.domain.valuerange.buildin.primlong; public class LongValueRange {}",
                                "package ai.timefold.solver.core.impl.domain.valuerange.buildin.temporal; public class TemporalValueRange {}",
                                "package ai.timefold.solver.core.impl.domain.valuerange.buildin; public class EmptyValueRange {}"));
    }

    @Test
    void migratePersistence() {
        rewriteRun(java(
                """
                        package timefold;

                        import ai.timefold.solver.persistence.common.api.domain.solution.SolutionFileIO;
                        import ai.timefold.solver.jpa.api.score.buildin.bendablebigdecimal.BendableBigDecimalScoreConverter;
                        import ai.timefold.solver.jpa.api.score.buildin.bendable.BendableScoreConverter;
                        import ai.timefold.solver.jpa.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScoreConverter;
                        import ai.timefold.solver.jpa.api.score.buildin.hardmediumsoft.HardMediumSoftScoreConverter;
                        import ai.timefold.solver.jpa.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScoreConverter;
                        import ai.timefold.solver.jpa.api.score.buildin.hardsoft.HardSoftScoreConverter;
                        import ai.timefold.solver.jpa.api.score.buildin.simplebigdecimal.SimpleBigDecimalScoreConverter;
                        import ai.timefold.solver.jpa.api.score.buildin.simple.SimpleScoreConverter;

                        public class Test {
                                SolutionFileIO solutionFileIO;
                                BendableBigDecimalScoreConverter bendableBigDecimalScoreConverter;
                                BendableScoreConverter bendableScoreConverter;
                                HardMediumSoftBigDecimalScoreConverter hardMediumSoftBigDecimalScoreConverter;
                                HardMediumSoftScoreConverter hardMediumSoftScoreConverter;
                                HardSoftBigDecimalScoreConverter hardSoftBigDecimalScoreConverter;
                                HardSoftScoreConverter hardSoftScoreConverter;
                                SimpleBigDecimalScoreConverter simpleBigDecimalScoreConverter;
                                SimpleScoreConverter simpleScoreConverter;
                        }""",
                """
                        package timefold;

                        import ai.timefold.solver.core.api.domain.solution.SolutionFileIO;
                        import ai.timefold.solver.jpa.api.score.BendableBigDecimalScoreConverter;
                        import ai.timefold.solver.jpa.api.score.BendableScoreConverter;
                        import ai.timefold.solver.jpa.api.score.HardMediumSoftBigDecimalScoreConverter;
                        import ai.timefold.solver.jpa.api.score.HardMediumSoftScoreConverter;
                        import ai.timefold.solver.jpa.api.score.HardSoftBigDecimalScoreConverter;
                        import ai.timefold.solver.jpa.api.score.HardSoftScoreConverter;
                        import ai.timefold.solver.jpa.api.score.SimpleBigDecimalScoreConverter;
                        import ai.timefold.solver.jpa.api.score.SimpleScoreConverter;

                        public class Test {
                                SolutionFileIO solutionFileIO;
                                BendableBigDecimalScoreConverter bendableBigDecimalScoreConverter;
                                BendableScoreConverter bendableScoreConverter;
                                HardMediumSoftBigDecimalScoreConverter hardMediumSoftBigDecimalScoreConverter;
                                HardMediumSoftScoreConverter hardMediumSoftScoreConverter;
                                HardSoftBigDecimalScoreConverter hardSoftBigDecimalScoreConverter;
                                HardSoftScoreConverter hardSoftScoreConverter;
                                SimpleBigDecimalScoreConverter simpleBigDecimalScoreConverter;
                                SimpleScoreConverter simpleScoreConverter;
                        }"""));
    }

    @Test
    void migrateJackson() {
        rewriteRun(java(
                """
                        package timefold;

                        import ai.timefold.solver.jackson.api.score.buildin.bendablebigdecimal.BendableBigDecimalScoreJacksonDeserializer;
                        import ai.timefold.solver.jackson.api.score.buildin.bendable.BendableScoreJacksonDeserializer;
                        import ai.timefold.solver.jackson.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScoreJacksonDeserializer;
                        import ai.timefold.solver.jackson.api.score.buildin.hardmediumsoft.HardMediumSoftScoreJacksonDeserializer;
                        import ai.timefold.solver.jackson.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScoreJacksonDeserializer;
                        import ai.timefold.solver.jackson.api.score.buildin.hardsoft.HardSoftScoreJacksonDeserializer;
                        import ai.timefold.solver.jackson.api.score.buildin.simplebigdecimal.SimpleBigDecimalScoreJacksonDeserializer;
                        import ai.timefold.solver.jackson.api.score.buildin.simple.SimpleScoreJacksonDeserializer;

                        public class Test {
                                BendableBigDecimalScoreJacksonDeserializer bendableBigDecimalScoreJacksonDeserializer;
                                BendableScoreJacksonDeserializer bendableScoreJacksonDeserializer;
                                HardMediumSoftBigDecimalScoreJacksonDeserializer hardMediumSoftBigDecimalScoreJacksonDeserializer;
                                HardMediumSoftScoreJacksonDeserializer hardMediumSoftScoreJacksonDeserializer;
                                HardSoftBigDecimalScoreJacksonDeserializer hardSoftBigDecimalScoreJacksonDeserializer;
                                HardSoftScoreJacksonDeserializer hardSoftScoreJacksonDeserializer;
                                SimpleBigDecimalScoreJacksonDeserializer simpleBigDecimalScoreJacksonDeserializer;
                                SimpleScoreJacksonDeserializer simpleScoreJacksonDeserializer;
                        }""",
                """
                        package timefold;

                        import ai.timefold.solver.jackson.api.score.buildin.BendableBigDecimalScoreJacksonDeserializer;
                        import ai.timefold.solver.jackson.api.score.buildin.BendableScoreJacksonDeserializer;
                        import ai.timefold.solver.jackson.api.score.buildin.HardMediumSoftBigDecimalScoreJacksonDeserializer;
                        import ai.timefold.solver.jackson.api.score.buildin.HardMediumSoftScoreJacksonDeserializer;
                        import ai.timefold.solver.jackson.api.score.buildin.HardSoftBigDecimalScoreJacksonDeserializer;
                        import ai.timefold.solver.jackson.api.score.buildin.HardSoftScoreJacksonDeserializer;
                        import ai.timefold.solver.jackson.api.score.buildin.SimpleBigDecimalScoreJacksonDeserializer;
                        import ai.timefold.solver.jackson.api.score.buildin.SimpleScoreJacksonDeserializer;

                        public class Test {
                                BendableBigDecimalScoreJacksonDeserializer bendableBigDecimalScoreJacksonDeserializer;
                                BendableScoreJacksonDeserializer bendableScoreJacksonDeserializer;
                                HardMediumSoftBigDecimalScoreJacksonDeserializer hardMediumSoftBigDecimalScoreJacksonDeserializer;
                                HardMediumSoftScoreJacksonDeserializer hardMediumSoftScoreJacksonDeserializer;
                                HardSoftBigDecimalScoreJacksonDeserializer hardSoftBigDecimalScoreJacksonDeserializer;
                                HardSoftScoreJacksonDeserializer hardSoftScoreJacksonDeserializer;
                                SimpleBigDecimalScoreJacksonDeserializer simpleBigDecimalScoreJacksonDeserializer;
                                SimpleScoreJacksonDeserializer simpleScoreJacksonDeserializer;
                        }"""));
    }

    @Test
    void migrateJaxb() {
        rewriteRun(java(
                """
                        package timefold;

                        import ai.timefold.solver.jaxb.api.score.buildin.bendablebigdecimal.BendableBigDecimalScoreJaxbAdapter;
                        import ai.timefold.solver.jaxb.api.score.buildin.bendable.BendableScoreJaxbAdapter;
                        import ai.timefold.solver.jaxb.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScoreJaxbAdapter;
                        import ai.timefold.solver.jaxb.api.score.buildin.hardmediumsoft.HardMediumSoftScoreJaxbAdapter;
                        import ai.timefold.solver.jaxb.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScoreJaxbAdapter;
                        import ai.timefold.solver.jaxb.api.score.buildin.hardsoft.HardSoftScoreJaxbAdapter;
                        import ai.timefold.solver.jaxb.api.score.buildin.simplebigdecimal.SimpleBigDecimalScoreJaxbAdapter;
                        import ai.timefold.solver.jaxb.api.score.buildin.simple.SimpleScoreJaxbAdapter;

                        public class Test {
                                BendableBigDecimalScoreJaxbAdapter bendableBigDecimalScoreJaxbAdapter;
                                BendableScoreJaxbAdapter bendableScoreJaxbAdapter;
                                HardMediumSoftBigDecimalScoreJaxbAdapter hardMediumSoftBigDecimalScoreJaxbAdapter;
                                HardMediumSoftScoreJaxbAdapter hardMediumSoftScoreJaxbAdapter;
                                HardSoftBigDecimalScoreJaxbAdapter hardSoftBigDecimalScoreJaxbAdapter;
                                HardSoftScoreJaxbAdapter hardSoftScoreJaxbAdapter;
                                SimpleBigDecimalScoreJaxbAdapter simpleBigDecimalScoreJaxbAdapter;
                                SimpleScoreJaxbAdapter simpleScoreJaxbAdapter;
                        }""",
                """
                        package timefold;

                        import ai.timefold.solver.jaxb.api.score.BendableBigDecimalScoreJaxbAdapter;
                        import ai.timefold.solver.jaxb.api.score.BendableScoreJaxbAdapter;
                        import ai.timefold.solver.jaxb.api.score.HardMediumSoftBigDecimalScoreJaxbAdapter;
                        import ai.timefold.solver.jaxb.api.score.HardMediumSoftScoreJaxbAdapter;
                        import ai.timefold.solver.jaxb.api.score.HardSoftBigDecimalScoreJaxbAdapter;
                        import ai.timefold.solver.jaxb.api.score.HardSoftScoreJaxbAdapter;
                        import ai.timefold.solver.jaxb.api.score.SimpleBigDecimalScoreJaxbAdapter;
                        import ai.timefold.solver.jaxb.api.score.SimpleScoreJaxbAdapter;

                        public class Test {
                                BendableBigDecimalScoreJaxbAdapter bendableBigDecimalScoreJaxbAdapter;
                                BendableScoreJaxbAdapter bendableScoreJaxbAdapter;
                                HardMediumSoftBigDecimalScoreJaxbAdapter hardMediumSoftBigDecimalScoreJaxbAdapter;
                                HardMediumSoftScoreJaxbAdapter hardMediumSoftScoreJaxbAdapter;
                                HardSoftBigDecimalScoreJaxbAdapter hardSoftBigDecimalScoreJaxbAdapter;
                                HardSoftScoreJaxbAdapter hardSoftScoreJaxbAdapter;
                                SimpleBigDecimalScoreJaxbAdapter simpleBigDecimalScoreJaxbAdapter;
                                SimpleScoreJaxbAdapter simpleScoreJaxbAdapter;
                        }"""));
    }

    @Test
    void migrateQuarkusJackson() {
        rewriteRun(java(
                """
                        package timefold;

                        import ai.timefold.solver.quarkus.jackson.score.buildin.bendablebigdecimal.BendableBigDecimalScoreJacksonDeserializer;
                        import ai.timefold.solver.quarkus.jackson.score.buildin.bendable.BendableScoreJacksonDeserializer;
                        import ai.timefold.solver.quarkus.jackson.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScoreJacksonDeserializer;
                        import ai.timefold.solver.quarkus.jackson.score.buildin.hardmediumsoft.HardMediumSoftScoreJacksonDeserializer;
                        import ai.timefold.solver.quarkus.jackson.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScoreJacksonDeserializer;
                        import ai.timefold.solver.quarkus.jackson.score.buildin.hardsoft.HardSoftScoreJacksonDeserializer;
                        import ai.timefold.solver.quarkus.jackson.score.buildin.simplebigdecimal.SimpleBigDecimalScoreJacksonDeserializer;
                        import ai.timefold.solver.quarkus.jackson.score.buildin.simple.SimpleScoreJacksonDeserializer;

                        public class Test {
                                BendableBigDecimalScoreJacksonDeserializer bendableBigDecimalScoreJacksonDeserializer;
                                BendableScoreJacksonDeserializer bendableScoreJacksonDeserializer;
                                HardMediumSoftBigDecimalScoreJacksonDeserializer hardMediumSoftBigDecimalScoreJacksonDeserializer;
                                HardMediumSoftScoreJacksonDeserializer hardMediumSoftScoreJacksonDeserializer;
                                HardSoftBigDecimalScoreJacksonDeserializer hardSoftBigDecimalScoreJacksonDeserializer;
                                HardSoftScoreJacksonDeserializer hardSoftScoreJacksonDeserializer;
                                SimpleBigDecimalScoreJacksonDeserializer simpleBigDecimalScoreJacksonDeserializer;
                                SimpleScoreJacksonDeserializer simpleScoreJacksonDeserializer;
                        }""",
                """
                        package timefold;

                        import ai.timefold.solver.quarkus.jackson.score.BendableBigDecimalScoreJacksonDeserializer;
                        import ai.timefold.solver.quarkus.jackson.score.BendableScoreJacksonDeserializer;
                        import ai.timefold.solver.quarkus.jackson.score.HardMediumSoftBigDecimalScoreJacksonDeserializer;
                        import ai.timefold.solver.quarkus.jackson.score.HardMediumSoftScoreJacksonDeserializer;
                        import ai.timefold.solver.quarkus.jackson.score.HardSoftBigDecimalScoreJacksonDeserializer;
                        import ai.timefold.solver.quarkus.jackson.score.HardSoftScoreJacksonDeserializer;
                        import ai.timefold.solver.quarkus.jackson.score.SimpleBigDecimalScoreJacksonDeserializer;
                        import ai.timefold.solver.quarkus.jackson.score.SimpleScoreJacksonDeserializer;

                        public class Test {
                                BendableBigDecimalScoreJacksonDeserializer bendableBigDecimalScoreJacksonDeserializer;
                                BendableScoreJacksonDeserializer bendableScoreJacksonDeserializer;
                                HardMediumSoftBigDecimalScoreJacksonDeserializer hardMediumSoftBigDecimalScoreJacksonDeserializer;
                                HardMediumSoftScoreJacksonDeserializer hardMediumSoftScoreJacksonDeserializer;
                                HardSoftBigDecimalScoreJacksonDeserializer hardSoftBigDecimalScoreJacksonDeserializer;
                                HardSoftScoreJacksonDeserializer hardSoftScoreJacksonDeserializer;
                                SimpleBigDecimalScoreJacksonDeserializer simpleBigDecimalScoreJacksonDeserializer;
                                SimpleScoreJacksonDeserializer simpleScoreJacksonDeserializer;
                        }"""));
    }

    @Test
    void migrateValueRange() {
        rewriteRun(java(
                """
                        package timefold;

                        import ai.timefold.solver.core.impl.domain.valuerange.buildin.bigdecimal.BigDecimalValueRange;
                        import ai.timefold.solver.core.impl.domain.valuerange.buildin.biginteger.BigIntegerValueRange;
                        import ai.timefold.solver.core.impl.domain.valuerange.buildin.primboolean.BooleanValueRange;
                        import ai.timefold.solver.core.impl.domain.valuerange.buildin.primint.IntValueRange;
                        import ai.timefold.solver.core.impl.domain.valuerange.buildin.collection.ListValueRange;
                        import ai.timefold.solver.core.impl.domain.valuerange.buildin.primlong.LongValueRange;
                        import ai.timefold.solver.core.impl.domain.valuerange.buildin.temporal.TemporalValueRange;
                        import ai.timefold.solver.core.impl.domain.valuerange.buildin.EmptyValueRange;

                        public class Test {
                                BigDecimalValueRange bigDecimalValueRange;
                                BigIntegerValueRange bigIntegerValueRange;
                                BooleanValueRange booleanValueRange;
                                IntValueRange intValueRange;
                                ListValueRange listValueRange;
                                LongValueRange longValueRange;
                                TemporalValueRange temporalValueRange;
                                EmptyValueRange emptyValueRange;
                        }""",
                """
                        package timefold;

                        import ai.timefold.solver.core.impl.domain.valuerange.BigDecimalValueRange;
                        import ai.timefold.solver.core.impl.domain.valuerange.BigIntegerValueRange;
                        import ai.timefold.solver.core.impl.domain.valuerange.BooleanValueRange;
                        import ai.timefold.solver.core.impl.domain.valuerange.IntValueRange;
                        import ai.timefold.solver.core.impl.domain.valuerange.ListValueRange;
                        import ai.timefold.solver.core.impl.domain.valuerange.LongValueRange;
                        import ai.timefold.solver.core.impl.domain.valuerange.TemporalValueRange;
                        import ai.timefold.solver.core.impl.domain.valuerange.EmptyValueRange;

                        public class Test {
                                BigDecimalValueRange bigDecimalValueRange;
                                BigIntegerValueRange bigIntegerValueRange;
                                BooleanValueRange booleanValueRange;
                                IntValueRange intValueRange;
                                ListValueRange listValueRange;
                                LongValueRange longValueRange;
                                TemporalValueRange temporalValueRange;
                                EmptyValueRange emptyValueRange;
                        }"""));
    }

}
