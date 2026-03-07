package ai.timefold.solver.migration.v2;

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
class GeneralTypeChangeMigrationRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(new GeneralTypeChangeMigrationRecipe())
                .typeValidationOptions(TypeValidation.builder().allowMissingType(ignore -> true).build())
                .parser(JavaParser.fromJavaVersion()
                        .styles(List.of(new NoWildCardImportStyle()))
                        // We must add all old classes as stubs to the JavaTemplate
                        .dependsOn("package ai.timefold.solver.core.api.domain.lookup; public class PlanningId {}",
                                "package ai.timefold.solver.core.api.score.director; public class ScoreDirector {}",
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
                                "package ai.timefold.solver.core.api.domain.valuerange; public class CountableValueRange {}",
                                "package ai.timefold.solver.core.api.domain.valuerange; public class ValueRange {}",
                                "package ai.timefold.solver.core.impl.domain.valuerange.buildin.composite; public class CompositeCountableValueRange {}",
                                "package ai.timefold.solver.core.impl.domain.valuerange.buildin.composite; public class NullAllowingCountableValueRange {}",
                                "package ai.timefold.solver.core.impl.heuristic.move; public interface Move {}",
                                "package ai.timefold.solver.core.impl.heuristic.move; public interface AbstractMove {}",
                                "package ai.timefold.solver.core.impl.heuristic.move; public interface NoChangeMove {}",
                                "package ai.timefold.solver.core.impl.heuristic.move; public interface CompositeMove {}",
                                "package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt; public interface KOptListMove {}",
                                "package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt; public interface TwoOptListMove {}",
                                "package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ruin; public interface ListRuinRecreateMove {}",
                                "package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list; public interface ListAssignMove {}",
                                "package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list; public interface ListChangeMove {}",
                                "package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list; public interface ListSwapMove {}",
                                "package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list; public interface ListUnassignMove {}",
                                "package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list; public interface SubListChangeMove {}",
                                "package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list; public interface SubListSwapMove {}",
                                "package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list; public interface SubListUnassignMove {}",
                                "package ai.timefold.solver.core.impl.heuristic.selector.move.generic; public interface ChangeMove {}",
                                "package ai.timefold.solver.core.impl.heuristic.selector.move.generic; public interface PillarChangeMove {}",
                                "package ai.timefold.solver.core.impl.heuristic.selector.move.generic; public interface PillarSwapMove {}",
                                "package ai.timefold.solver.core.impl.heuristic.selector.move.generic; public interface RuinRecreateMove {}",
                                "package ai.timefold.solver.core.impl.heuristic.selector.move.generic; public interface SwapMove {}"));
    }

    @Test
    void migrate() {
        rewriteRun(java(
                """
                        package timefold;

                        import ai.timefold.solver.core.api.domain.lookup.PlanningId;
                        import ai.timefold.solver.core.api.solver.ProblemFactChange;

                        public class Test {
                                @PlanningId
                                SimpleScore simpleScore;
                                ProblemFactChange  problemFactChange;
                        }""",
                """
                        package timefold;

                        import ai.timefold.solver.core.api.domain.common.PlanningId;
                        import ai.timefold.solver.core.api.solver.change.ProblemChange;

                        public class Test {
                                @PlanningId
                                SimpleScore simpleScore;
                                ProblemChange  problemFactChange;
                        }"""));
    }

    @Test
    void migrateScore() {
        rewriteRun(java(
                """
                        package timefold;

                        import ai.timefold.solver.core.api.score.director.ScoreDirector;
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
                                ScoreDirector scoreDirector;
                                SimpleScore simpleScore;
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
                        }""",
                """
                        package timefold;

                        import ai.timefold.solver.core.impl.score.director.ScoreDirector;
                        import ai.timefold.solver.core.api.score.BendableBigDecimalScore;
                        import ai.timefold.solver.core.api.score.BendableScore;
                        import ai.timefold.solver.core.api.score.HardMediumSoftBigDecimalScore;
                        import ai.timefold.solver.core.api.score.HardMediumSoftScore;
                        import ai.timefold.solver.core.api.score.HardSoftBigDecimalScore;
                        import ai.timefold.solver.core.api.score.HardSoftScore;
                        import ai.timefold.solver.core.api.score.SimpleBigDecimalScore;
                        import ai.timefold.solver.core.api.score.SimpleScore;

                        public class Test {
                                ScoreDirector scoreDirector;
                                SimpleScore simpleScore;
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
                        }"""));
    }

    @Test
    void migrateValueRange() {
        rewriteRun(java(
                """
                        package timefold;

                        import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
                        import ai.timefold.solver.core.impl.domain.valuerange.buildin.composite.CompositeCountableValueRange;
                        import ai.timefold.solver.core.impl.domain.valuerange.buildin.composite.NullAllowingCountableValueRange;

                        public class Test {
                                CountableValueRange valueRange;
                                CompositeCountableValueRange valueRange2;
                                NullAllowingCountableValueRange valueRange3;
                        }""",
                """
                        package timefold;

                        import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
                        import ai.timefold.solver.core.impl.domain.valuerange.CompositeValueRange;
                        import ai.timefold.solver.core.impl.domain.valuerange.NullAllowingValueRange;

                        public class Test {
                                ValueRange valueRange;
                                CompositeValueRange valueRange2;
                                NullAllowingValueRange valueRange3;
                        }"""));
    }

    @Test
    void migrateMove() {
        rewriteRun(java(
                """
                        package timefold;

                        import ai.timefold.solver.core.impl.heuristic.move.Move;
                        import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
                        import ai.timefold.solver.core.impl.heuristic.move.NoChangeMove;
                        import ai.timefold.solver.core.impl.heuristic.move.CompositeMove;
                        import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt.KOptListMove;
                        import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt.TwoOptListMove;
                        import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ruin.ListRuinRecreateMove;
                        import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListAssignMove;
                        import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListChangeMove;
                        import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListSwapMove;
                        import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListUnassignMove;
                        import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SubListChangeMove;
                        import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SubListSwapMove;
                        import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SubListUnassignMove;
                        import ai.timefold.solver.core.impl.heuristic.selector.move.generic.ChangeMove;
                        import ai.timefold.solver.core.impl.heuristic.selector.move.generic.PillarChangeMove;
                        import ai.timefold.solver.core.impl.heuristic.selector.move.generic.PillarSwapMove;
                        import ai.timefold.solver.core.impl.heuristic.selector.move.generic.RuinRecreateMove;
                        import ai.timefold.solver.core.impl.heuristic.selector.move.generic.SwapMove;

                        public abstract class Test implements AbstractMove, Move {
                                NoChangeMove noChangeMove;
                                CompositeMove compositeMove;
                                KOptListMove kOptListMove;
                                TwoOptListMove twoOptListMove;
                                ListRuinRecreateMove listRuinRecreateMove;
                                ListAssignMove listAssignMove;
                                ListChangeMove listChangeMove;
                                ListSwapMove listSwapMove;
                                ListUnassignMove listUnassignMove;
                                SubListChangeMove subListChangeMove;
                                SubListSwapMove subListSwapMove;
                                SubListUnassignMove subListUnassignMove;
                                ChangeMove changeMove;
                                PillarChangeMove pillarChangeMove;
                                PillarSwapMove PillarSwapMove;
                                RuinRecreateMove ruinRecreateMove;
                                SwapMove swapMove;
                        }""",
                """
                        package timefold;

                        import ai.timefold.solver.core.impl.heuristic.move.AbstractSelectorBasedMove;
                        import ai.timefold.solver.core.impl.heuristic.move.SelectorBasedCompositeMove;
                        import ai.timefold.solver.core.impl.heuristic.move.SelectorBasedNoChangeMove;
                        import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SelectorBasedSubListUnassignMove;
                        import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt.SelectorBasedKOptListMove;
                        import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt.SelectorBasedTwoOptListMove;
                        import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ruin.SelectorBasedListRuinRecreateMove;
                        import ai.timefold.solver.core.preview.api.move.Move;
                        import ai.timefold.solver.core.impl.heuristic.selector.move.generic.SelectorBasedSwapMove;
                        import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SelectorBasedListAssignMove;
                        import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SelectorBasedListChangeMove;
                        import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SelectorBasedListSwapMove;
                        import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SelectorBasedListUnassignMove;
                        import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SelectorBasedSubListChangeMove;
                        import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SelectorBasedSubListSwapMove;
                        import ai.timefold.solver.core.impl.heuristic.selector.move.generic.SelectorBasedChangeMove;
                        import ai.timefold.solver.core.impl.heuristic.selector.move.generic.SelectorBasedPillarChangeMove;
                        import ai.timefold.solver.core.impl.heuristic.selector.move.generic.SelectorBasedPillarSwapMove;
                        import ai.timefold.solver.core.impl.heuristic.selector.move.generic.SelectorBasedRuinRecreateMove;

                        public abstract class Test implements AbstractSelectorBasedMove, Move {
                                SelectorBasedNoChangeMove noChangeMove;
                                SelectorBasedCompositeMove compositeMove;
                                SelectorBasedKOptListMove kOptListMove;
                                SelectorBasedTwoOptListMove twoOptListMove;
                                SelectorBasedListRuinRecreateMove listRuinRecreateMove;
                                SelectorBasedListAssignMove listAssignMove;
                                SelectorBasedListChangeMove listChangeMove;
                                SelectorBasedListSwapMove listSwapMove;
                                SelectorBasedListUnassignMove listUnassignMove;
                                SelectorBasedSubListChangeMove subListChangeMove;
                                SelectorBasedSubListSwapMove subListSwapMove;
                                SelectorBasedSubListUnassignMove subListUnassignMove;
                                SelectorBasedChangeMove changeMove;
                                SelectorBasedPillarChangeMove pillarChangeMove;
                                SelectorBasedPillarSwapMove SelectorBasedPillarSwapMove;
                                SelectorBasedRuinRecreateMove ruinRecreateMove;
                                SelectorBasedSwapMove swapMove;
                        }"""));
    }
}
