package ai.timefold.solver.migration.v8;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.migration.AbstractRecipe;

import org.openrewrite.Recipe;
import org.openrewrite.java.ReorderMethodArguments;

public final class SingleConstraintAssertionMethodsRecipe extends AbstractRecipe {
    @Override
    public String getDisplayName() {
        return "Use non-deprecated SingleConstraintAssertion methods";
    }

    @Override
    public String getDescription() {
        return "Use `penalizesBy/rewardsWith(String, int)` instead of `penalizesBy/rewardsWith(int, String)` on `SingleConstraintAssertion` tests.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        var methodsToReorder = List.of(
                new String[] { "ai.timefold.solver.test.api.score.stream.SingleConstraintAssertion *(int, String)",
                        "message,matchWeightTotal",
                        "matchWeightTotal,message" },
                new String[] { "ai.timefold.solver.test.api.score.stream.SingleConstraintAssertion *(long, String)",
                        "message,matchWeightTotal",
                        "matchWeightTotal,message" },
                new String[] {
                        "ai.timefold.solver.test.api.score.stream.SingleConstraintAssertion *(java.math.BigDecimal, String)",
                        "message,matchWeightTotal",
                        "matchWeightTotal,message" });
        var result = new ArrayList<Recipe>();
        methodsToReorder.forEach(method -> result
                .add(new ReorderMethodArguments(method[0], method[1].split(","), method[2].split(","), null, null)));
        return result;
    }
}
