package ai.timefold.solver.migration.v2;

import java.util.List;

import ai.timefold.solver.migration.AbstractRecipe;

import org.openrewrite.Recipe;
import org.openrewrite.java.ChangeType;

public class GeneralTypeChangeMigrationRecipe extends AbstractRecipe {
    @Override
    public String getDisplayName() {
        return "Migrate legacy code to the new class structure";
    }

    @Override
    public String getDescription() {
        return "Migrate all legacy classes to the new class structure.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                // Planning Id
                new ChangeType("ai.timefold.solver.core.api.domain.lookup.PlanningId",
                        "ai.timefold.solver.core.api.domain.common.PlanningId", true),
                // Score API
                new ChangeType("ai.timefold.solver.core.api.score.director.ScoreDirector",
                        "ai.timefold.solver.core.impl.score.director.ScoreDirector", true),
                new ChangeType("ai.timefold.solver.core.api.score.buildin.simple.SimpleScore",
                        "ai.timefold.solver.core.api.score.SimpleScore", true),
                new ChangeType("ai.timefold.solver.core.api.score.buildin.simplelong.SimpleLongScore",
                        "ai.timefold.solver.core.api.score.SimpleScore", true),
                new ChangeType("ai.timefold.solver.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore",
                        "ai.timefold.solver.core.api.score.SimpleBigDecimalScore", true),

                new ChangeType("ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore",
                        "ai.timefold.solver.core.api.score.HardSoftScore", true),
                new ChangeType("ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore",
                        "ai.timefold.solver.core.api.score.HardSoftScore", true),
                new ChangeType("ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore",
                        "ai.timefold.solver.core.api.score.HardSoftBigDecimalScore", true),

                new ChangeType("ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore",
                        "ai.timefold.solver.core.api.score.HardMediumSoftScore", true),
                new ChangeType("ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore",
                        "ai.timefold.solver.core.api.score.HardMediumSoftScore", true),
                new ChangeType(
                        "ai.timefold.solver.core.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScore",
                        "ai.timefold.solver.core.api.score.HardMediumSoftBigDecimalScore", true),

                new ChangeType("ai.timefold.solver.core.api.score.buildin.bendable.BendableScore",
                        "ai.timefold.solver.core.api.score.BendableScore", true),
                new ChangeType("ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore",
                        "ai.timefold.solver.core.api.score.BendableScore", true),
                new ChangeType("ai.timefold.solver.core.api.score.buildin.bendablebigdecimal.BendableBigDecimalScore",
                        "ai.timefold.solver.core.api.score.BendableBigDecimalScore", true),
                // Problem fact
                new ChangeType("ai.timefold.solver.core.api.solver.ProblemFactChange",
                        "ai.timefold.solver.core.api.solver.change.ProblemChange", true),
                // Value Range
                new ChangeType("ai.timefold.solver.core.api.domain.valuerange.CountableValueRange",
                        "ai.timefold.solver.core.api.domain.valuerange.ValueRange", true),
                new ChangeType("ai.timefold.solver.core.impl.domain.valuerange.buildin.composite.CompositeCountableValueRange",
                        "ai.timefold.solver.core.impl.domain.valuerange.CompositeValueRange", true),
                new ChangeType(
                        "ai.timefold.solver.core.impl.domain.valuerange.buildin.composite.NullAllowingCountableValueRange",
                        "ai.timefold.solver.core.impl.domain.valuerange.NullAllowingValueRange", true),
                // Move API
                new ChangeType("ai.timefold.solver.core.impl.heuristic.move.Move",
                        "ai.timefold.solver.core.preview.api.move.Move", true),
                new ChangeType(
                        "ai.timefold.solver.core.impl.heuristic.move.AbstractMove",
                        "ai.timefold.solver.core.impl.heuristic.move.AbstractSelectorBasedMove", true),
                new ChangeType(
                        "ai.timefold.solver.core.impl.heuristic.move.NoChangeMove",
                        "ai.timefold.solver.core.impl.heuristic.move.SelectorBasedNoChangeMove", true),
                new ChangeType(
                        "ai.timefold.solver.core.impl.heuristic.move.CompositeMove",
                        "ai.timefold.solver.core.impl.heuristic.move.SelectorBasedCompositeMove", true),
                new ChangeType(
                        "ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt.KOptListMove",
                        "ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt.SelectorBasedKOptListMove",
                        true),
                new ChangeType(
                        "ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt.TwoOptListMove",
                        "ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt.SelectorBasedTwoOptListMove",
                        true),
                new ChangeType(
                        "ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ruin.ListRuinRecreateMove",
                        "ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ruin.SelectorBasedListRuinRecreateMove",
                        true),
                new ChangeType(
                        "ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListAssignMove",
                        "ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SelectorBasedListAssignMove", true),
                new ChangeType(
                        "ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListChangeMove",
                        "ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SelectorBasedListChangeMove", true),
                new ChangeType(
                        "ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListSwapMove",
                        "ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SelectorBasedListSwapMove", true),
                new ChangeType(
                        "ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListUnassignMove",
                        "ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SelectorBasedListUnassignMove",
                        true),
                new ChangeType(
                        "ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SubListChangeMove",
                        "ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SelectorBasedSubListChangeMove",
                        true),
                new ChangeType(
                        "ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SubListSwapMove",
                        "ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SelectorBasedSubListSwapMove", true),
                new ChangeType(
                        "ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SubListUnassignMove",
                        "ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SelectorBasedSubListUnassignMove",
                        true),
                new ChangeType(
                        "ai.timefold.solver.core.impl.heuristic.selector.move.generic.ChangeMove",
                        "ai.timefold.solver.core.impl.heuristic.selector.move.generic.SelectorBasedChangeMove", true),
                new ChangeType(
                        "ai.timefold.solver.core.impl.heuristic.selector.move.generic.PillarChangeMove",
                        "ai.timefold.solver.core.impl.heuristic.selector.move.generic.SelectorBasedPillarChangeMove", true),
                new ChangeType(
                        "ai.timefold.solver.core.impl.heuristic.selector.move.generic.PillarSwapMove",
                        "ai.timefold.solver.core.impl.heuristic.selector.move.generic.SelectorBasedPillarSwapMove", true),
                new ChangeType(
                        "ai.timefold.solver.core.impl.heuristic.selector.move.generic.RuinRecreateMove",
                        "ai.timefold.solver.core.impl.heuristic.selector.move.generic.SelectorBasedRuinRecreateMove", true),
                new ChangeType(
                        "ai.timefold.solver.core.impl.heuristic.selector.move.generic.SwapMove",
                        "ai.timefold.solver.core.impl.heuristic.selector.move.generic.SelectorBasedSwapMove", true));
    }
}
