package ai.timefold.solver.migration.preview;

import static org.openrewrite.java.Assertions.java;

import java.util.List;

import ai.timefold.solver.migration.NoWildCardImportStyle;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

@Execution(ExecutionMode.CONCURRENT)
class NeighborhoodsMigrationRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(new NeighborhoodsMigrationRecipe())
                .typeValidationOptions(TypeValidation.builder().allowMissingType(ignore -> true).build())
                .parser(JavaParser.fromJavaVersion()
                        .styles(List.of(new NoWildCardImportStyle()))
                        // We must add all old classes as stubs to the JavaTemplate
                        .dependsOn(
                                "package ai.timefold.solver.core.preview.api.neighborhood.stream.sampling; public interface SamplingStream {}",
                                "package ai.timefold.solver.core.preview.api.neighborhood.stream.sampling; public interface UniSamplingStream<Solution_, A> {}",
                                "package ai.timefold.solver.core.preview.api.neighborhood.stream.sampling; public interface BiSamplingStream<Solution_, A, B> {}",
                                "package ai.timefold.solver.core.impl.neighborhood.stream.sampling; public interface InnerSamplingStream<Solution_> {}",
                                "package ai.timefold.solver.core.impl.neighborhood.stream.sampling; public interface InnerUniSamplingStream<Solution_, A> {}",
                                "package ai.timefold.solver.core.impl.neighborhood.stream.sampling; public class DefaultUniSamplingStream<Solution_, A> {}",
                                "package ai.timefold.solver.core.impl.neighborhood.stream.sampling; public class DefaultBiSamplingStream<Solution_, A, B> {}"));
    }

    @Test
    void migratePickingStream() {
        rewriteRun(java(
                """
                        package timefold;

                        import ai.timefold.solver.core.preview.api.neighborhood.stream.sampling.BiSamplingStream;
                        import ai.timefold.solver.core.preview.api.neighborhood.stream.sampling.SamplingStream;
                        import ai.timefold.solver.core.preview.api.neighborhood.stream.sampling.UniSamplingStream;

                        public class Test {
                                SamplingStream samplingStream;
                                UniSamplingStream<Object, Object> uniSamplingStream;
                                BiSamplingStream<Object, Object, Object> biSamplingStream;
                        }""",
                """
                        package timefold;

                        import ai.timefold.solver.core.preview.api.neighborhood.stream.picking.BiPickingStream;
                        import ai.timefold.solver.core.preview.api.neighborhood.stream.picking.PickingStream;
                        import ai.timefold.solver.core.preview.api.neighborhood.stream.picking.UniPickingStream;

                        public class Test {
                                PickingStream samplingStream;
                                UniPickingStream<Object, Object> uniSamplingStream;
                                BiPickingStream<Object, Object, Object> biSamplingStream;
                        }"""));
    }

    @Test
    void migrateInnerPickingStream() {
        rewriteRun(java(
                """
                        package timefold;

                        import ai.timefold.solver.core.impl.neighborhood.stream.sampling.DefaultBiSamplingStream;
                        import ai.timefold.solver.core.impl.neighborhood.stream.sampling.DefaultUniSamplingStream;
                        import ai.timefold.solver.core.impl.neighborhood.stream.sampling.InnerSamplingStream;
                        import ai.timefold.solver.core.impl.neighborhood.stream.sampling.InnerUniSamplingStream;

                        public class Test {
                                InnerSamplingStream<Object> innerSamplingStream;
                                InnerUniSamplingStream<Object, Object> innerUniSamplingStream;
                                DefaultUniSamplingStream<Object, Object> defaultUniSamplingStream;
                                DefaultBiSamplingStream<Object, Object, Object> defaultBiSamplingStream;
                        }""",
                """
                        package timefold;

                        import ai.timefold.solver.core.impl.neighborhood.stream.picking.DefaultBiPickingStream;
                        import ai.timefold.solver.core.impl.neighborhood.stream.picking.DefaultUniPickingStream;
                        import ai.timefold.solver.core.impl.neighborhood.stream.picking.InnerPickingStream;
                        import ai.timefold.solver.core.impl.neighborhood.stream.picking.InnerUniPickingStream;

                        public class Test {
                                InnerPickingStream<Object> innerSamplingStream;
                                InnerUniPickingStream<Object, Object> innerUniSamplingStream;
                                DefaultUniPickingStream<Object, Object> defaultUniSamplingStream;
                                DefaultBiPickingStream<Object, Object, Object> defaultBiSamplingStream;
                        }"""));
    }

}
