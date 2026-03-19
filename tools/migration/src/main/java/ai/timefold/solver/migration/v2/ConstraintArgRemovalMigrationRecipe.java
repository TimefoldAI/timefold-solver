package ai.timefold.solver.migration.v2;

import java.util.List;

import ai.timefold.solver.migration.AbstractRecipe;

import org.openrewrite.Recipe;
import org.openrewrite.java.DeleteMethodArgument;

public final class ConstraintArgRemovalMigrationRecipe extends AbstractRecipe {

    @Override
    public String getDisplayName() {
        return "Remove constraint package argument from ConstraintRef and ConstraintBuilder";
    }

    @Override
    public String getDescription() {
        return "Removes the first (package) argument from ConstraintRef.of() and ConstraintBuilder.asConstraint().";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new DeleteMethodArgument(
                        "ai.timefold.solver.core.api.score.constraint.ConstraintRef of(String, String)", 0),
                new DeleteMethodArgument(
                        "ai.timefold.solver.core.api.score.stream.ConstraintBuilder asConstraint(String, String)", 0));
    }

}