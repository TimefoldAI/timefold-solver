package ai.timefold.solver.migration.v8;

import java.util.List;

import ai.timefold.solver.migration.AbstractRecipe;

import org.openrewrite.Recipe;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.ChangeType;

public final class SolutionManagerRecommendAssignmentRecipe extends AbstractRecipe {
    @Override
    public String getDisplayName() {
        return "Recommended Fit API becomes Assignment Recommendation API";
    }

    @Override
    public String getDescription() {
        return "Use recommendAssignment() instead of recommendFit().";
    }

    @Override
    public List<Recipe> getRecipeList() {
        var changeMethodName = new ChangeMethodName(
                "ai.timefold.solver.core.api.solver.SolutionManager recommendFit(" +
                        "java.lang.Object, " +
                        "java.lang.Object, " +
                        "java.util.function.Function)",
                "recommendAssignment", true, false);
        var changeMethodName2 = new ChangeMethodName(
                "ai.timefold.solver.core.api.solver.SolutionManager recommendFit(" +
                        "java.lang.Object, java.lang.Object, " +
                        "java.util.function.Function, " +
                        "ai.timefold.solver.core.api.solver.ScoreAnalysisFetchPolicy)",
                "recommendAssignment", true, false);
        var changeType = new ChangeType("ai.timefold.solver.core.api.solver.RecommendedFit",
                "ai.timefold.solver.core.api.solver.RecommendedAssignment", false);
        return List.of(changeMethodName, changeMethodName2, changeType);
    }
}
