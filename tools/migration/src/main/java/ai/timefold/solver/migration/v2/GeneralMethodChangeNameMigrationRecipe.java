package ai.timefold.solver.migration.v2;

import java.util.List;

import ai.timefold.solver.migration.AbstractRecipe;
import ai.timefold.solver.migration.common.CustomChangeMethodRecipe;

import org.openrewrite.Recipe;
import org.openrewrite.java.ChangeMethodName;

public class GeneralMethodChangeNameMigrationRecipe extends AbstractRecipe {
    @Override
    public String getDisplayName() {
        return "Migrate legacy methods to the new structure";
    }

    @Override
    public String getDescription() {
        return "Migrate all legacy methods to the new structure.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                // PlannerBenchmark
                new ChangeMethodName(
                        "ai.timefold.solver.benchmark.api.PlannerBenchmark benchmarkAndShowReportInBrowser()",
                        "benchmark", true, false),
                // Constraint
                new CustomChangeMethodRecipe("ai.timefold.solver.core.api.score.stream.Constraint",
                        "getConstraintName()",
                        ".getConstraintRef().constraintName()"),
                // Constraint
                new ChangeMethodName("ai.timefold.solver.core.api.solver.Solver isEveryProblemFactChangeProcessed()",
                        "isEveryProblemChangeProcessed", true, false),
                // BestSolutionChangedEvent
                new ChangeMethodName(
                        "ai.timefold.solver.core.api.solver.event.BestSolutionChangedEvent isEveryProblemFactChangeProcessed()",
                        "isEveryProblemChangeProcessed", true, false),
                // Move API
                new ChangeMethodName(
                        "ai.timefold.solver.core.config.heuristic.selector.move.composite.CartesianProductMoveSelectorConfig getMoveSelectorConfigList()",
                        "getMoveSelectorList", true, false),
                new ChangeMethodName(
                        "ai.timefold.solver.core.config.heuristic.selector.move.composite.CartesianProductMoveSelectorConfig setMoveSelectorConfigList(..)",
                        "setMoveSelectorList", true, false),
                new ChangeMethodName(
                        "ai.timefold.solver.core.config.heuristic.selector.move.composite.UnionMoveSelectorConfig getMoveSelectorConfigList()",
                        "getMoveSelectorList", true, false),
                new ChangeMethodName(
                        "ai.timefold.solver.core.config.heuristic.selector.move.composite.UnionMoveSelectorConfig setMoveSelectorConfigList(..)",
                        "setMoveSelectorList", true, false),
                // Constraint Stream API
                new ChangeMethodName(
                        "ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream penalizeLong(..)",
                        "penalize", true, false),
                new ChangeMethodName(
                        "ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream rewardLong(..)",
                        "reward", true, false),
                new ChangeMethodName(
                        "ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream impactLong(..)",
                        "impact", true, false),
                new ChangeMethodName(
                        "ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream penalizeLong(..)",
                        "penalize", true, false),
                new ChangeMethodName(
                        "ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream rewardLong(..)",
                        "reward", true, false),
                new ChangeMethodName(
                        "ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream impactLong(..)",
                        "impact", true, false),
                new ChangeMethodName(
                        "ai.timefold.solver.core.api.score.stream.tri.TriConstraintStream penalizeLong(..)",
                        "penalize", true, false),
                new ChangeMethodName(
                        "ai.timefold.solver.core.api.score.stream.tri.TriConstraintStream rewardLong(..)",
                        "reward", true, false),
                new ChangeMethodName(
                        "ai.timefold.solver.core.api.score.stream.tri.TriConstraintStream impactLong(..)",
                        "impact", true, false),
                new ChangeMethodName(
                        "ai.timefold.solver.core.api.score.stream.quad.QuadConstraintStream penalizeLong(..)",
                        "penalize", true, false),
                new ChangeMethodName(
                        "ai.timefold.solver.core.api.score.stream.quad.QuadConstraintStream rewardLong(..)",
                        "reward", true, false),
                new ChangeMethodName(
                        "ai.timefold.solver.core.api.score.stream.quad.QuadConstraintStream impactLong(..)",
                        "impact", true, false),
                // ConstraintCollectors – Long suffix removed
                new ChangeMethodName(
                        "ai.timefold.solver.core.api.score.stream.ConstraintCollectors countLong()",
                        "count", true, false),
                new ChangeMethodName(
                        "ai.timefold.solver.core.api.score.stream.ConstraintCollectors countLongBi()",
                        "countBi", true, false),
                new ChangeMethodName(
                        "ai.timefold.solver.core.api.score.stream.ConstraintCollectors countLongTri()",
                        "countTri", true, false),
                new ChangeMethodName(
                        "ai.timefold.solver.core.api.score.stream.ConstraintCollectors countLongQuad()",
                        "countQuad", true, false),
                new ChangeMethodName(
                        "ai.timefold.solver.core.api.score.stream.ConstraintCollectors countDistinctLong(..)",
                        "countDistinct", true, false),
                new ChangeMethodName(
                        "ai.timefold.solver.core.api.score.stream.ConstraintCollectors sumLong(..)",
                        "sum", true, false),
                new ChangeMethodName(
                        "ai.timefold.solver.core.api.score.stream.ConstraintCollectors averageLong(..)",
                        "average", true, false));

    }
}
