package ai.timefold.solver.migration.v8;

import java.util.ArrayList;
import java.util.List;

import org.openrewrite.Recipe;
import org.openrewrite.java.ReorderMethodArguments;

public final class SingleConstraintAssertionMethodsRecipe extends Recipe {
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
                new String[] { "ai.timefold.solver.test.api.score.stream.SingleConstraintAssertion *(int,String)",
                        "matchWeightTotal,message",
                        "message,matchWeightTotal" },
                new String[] { "ai.timefold.solver.test.api.score.stream.SingleConstraintAssertion *(long, String)",
                        "matchWeightTotal,message",
                        "message,matchWeightTotal" },
                new String[] {
                        "ai.timefold.solver.test.api.score.stream.SingleConstraintAssertion *(java.math.BigDecimal, String)",
                        "matchWeightTotal,message",
                        "message,matchWeightTotal" });
        var result = new ArrayList<Recipe>();
        methodsToReorder.forEach(method -> result
                .add(new ReorderMethodArguments(method[0], method[1].split(","), method[2].split(","), null, null)));
        return result;
    }
}
