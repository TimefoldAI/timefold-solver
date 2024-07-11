package ai.timefold.solver.migration.v8;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ai.timefold.solver.migration.AbstractRecipe;

import org.openrewrite.Recipe;
import org.openrewrite.java.ChangeAnnotationAttributeName;
import org.openrewrite.java.ChangeMethodName;

public final class NullableRecipe extends AbstractRecipe {

    @Override
    public String getDisplayName() {
        return "PlanningVariable's `nullable` is newly called `unassignedValues`";
    }

    @Override
    public String getDescription() {
        return "Removes references to null vars and replace them with unassigned values.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        var packageName = "ai.timefold.solver.core.api.score.stream";
        var methodsToRename = Map.ofEntries(
                Map.entry("ConstraintFactory forEachIncludingNullVars(Class)",
                        "forEachIncludingUnassigned"),
                Map.entry("uni.UniConstraintStream ifExistsIncludingNullVars(..)",
                        "ifExistsIncludingUnassigned"),
                Map.entry("uni.UniConstraintStream ifExistsOtherIncludingNullVars(..)",
                        "ifExistsOtherIncludingUnassigned"),
                Map.entry("uni.UniConstraintStream ifNotExistsIncludingNullVars(..)",
                        "ifNotExistsIncludingUnassigned"),
                Map.entry("uni.UniConstraintStream ifNotExistsOtherIncludingNullVars(..)",
                        "ifNotExistsOtherIncludingUnassigned"),
                Map.entry("bi.BiConstraintStream ifExistsIncludingNullVars(..)",
                        "ifExistsIncludingUnassigned"),
                Map.entry("bi.BiConstraintStream ifNotExistsIncludingNullVars(..)",
                        "ifNotExistsIncludingUnassigned"),
                Map.entry("tri.TriConstraintStream ifExistsIncludingNullVars(..)",
                        "ifExistsIncludingUnassigned"),
                Map.entry("tri.TriConstraintStream ifNotExistsIncludingNullVars(..)",
                        "ifNotExistsIncludingUnassigned"),
                Map.entry("quad.QuadConstraintStream ifExistsIncludingNullVars(..)",
                        "ifExistsIncludingUnassigned"),
                Map.entry("quad.QuadConstraintStream ifNotExistsIncludingNullVars(..)",
                        "ifNotExistsIncludingUnassigned"));

        var result = new ArrayList<Recipe>();
        methodsToRename.forEach((oldPattern, newMethodName) -> {
            var pattern = packageName + "." + oldPattern;
            result.add(new ChangeMethodName(pattern, newMethodName, null, null));
        });
        result.add(new ChangeAnnotationAttributeName("ai.timefold.solver.core.api.domain.variable.PlanningVariable", "nullable",
                "allowsUnassigned"));
        return result;
    }

}
