package ai.timefold.solver.migration.v2;

import java.util.List;

import ai.timefold.solver.migration.AbstractRecipe;

import org.openrewrite.Recipe;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.DeleteMethodArgument;
import org.openrewrite.java.RemoveMethodInvocations;

public final class ConstraintMetadataMigrationRecipe extends AbstractRecipe {

    @Override
    public String getDisplayName() {
        return "Migrate constraint description and group APIs";
    }

    @Override
    public String getDescription() {
        return "Renames asConstraintDescribed to asConstraint, updates ConstraintRef.constraintName() to id(), "
                + "ConstraintAnalysis.constraintName() to constraintId(), and removes deleted group/description methods.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                // asConstraintDescribed(String name, String description) -> asConstraint(String name), drop arg 1
                new ChangeMethodName(
                        "ai.timefold.solver.core.api.score.stream.ConstraintBuilder asConstraintDescribed(String, String)",
                        "asConstraint", true, false),
                new DeleteMethodArgument(
                        "ai.timefold.solver.core.api.score.stream.ConstraintBuilder asConstraint(String, String)",
                        1),
                // asConstraintDescribed(String name, String group, String description) -> asConstraint(String name), drop args 1 and 2
                new ChangeMethodName(
                        "ai.timefold.solver.core.api.score.stream.ConstraintBuilder asConstraintDescribed(String, String, String)",
                        "asConstraint", true, false),
                new DeleteMethodArgument(
                        "ai.timefold.solver.core.api.score.stream.ConstraintBuilder asConstraint(String, String, String)",
                        2),
                new DeleteMethodArgument(
                        "ai.timefold.solver.core.api.score.stream.ConstraintBuilder asConstraint(String, String)",
                        1),
                // ConstraintRef.constraintName() -> id()
                new ChangeMethodName(
                        "ai.timefold.solver.core.api.score.stream.ConstraintRef constraintName()",
                        "id", true, false),
                // ConstraintAnalysis.constraintName() -> constraintId()
                new ChangeMethodName(
                        "ai.timefold.solver.core.api.score.analysis.ConstraintAnalysis constraintName()",
                        "constraintId", true, false),
                // Constraint.getConstraintGroup() removed
                new RemoveMethodInvocations(
                        "ai.timefold.solver.core.api.score.stream.Constraint getConstraintGroup()"),
                // Constraint.getDescription() returning String removed
                new RemoveMethodInvocations(
                        "ai.timefold.solver.core.api.score.stream.Constraint getDescription()"),
                // ConstraintMetaModel.getConstraintsPerGroup(..) removed
                new RemoveMethodInvocations(
                        "ai.timefold.solver.core.api.score.stream.ConstraintMetaModel getConstraintsPerGroup(..)"),
                // ConstraintMetaModel.getConstraintGroups() removed
                new RemoveMethodInvocations(
                        "ai.timefold.solver.core.api.score.stream.ConstraintMetaModel getConstraintGroups()"));
    }

}
