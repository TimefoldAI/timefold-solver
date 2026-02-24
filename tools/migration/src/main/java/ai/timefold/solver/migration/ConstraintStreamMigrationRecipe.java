package ai.timefold.solver.migration;

import java.util.List;

import org.openrewrite.Recipe;
import org.openrewrite.java.ChangeMethodName;

public class ConstraintStreamMigrationRecipe extends AbstractRecipe {
    @Override
    public String getDisplayName() {
        return "Migrate legacy constraint stream code to the new methods";
    }

    @Override
    public String getDescription() {
        return "Migrate all legacy constraint stream methods to the new methods.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
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
                        "impact", true, false));
    }
}
