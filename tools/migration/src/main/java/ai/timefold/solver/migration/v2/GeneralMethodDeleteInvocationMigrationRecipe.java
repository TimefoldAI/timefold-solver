package ai.timefold.solver.migration.v2;

import java.util.List;

import ai.timefold.solver.migration.AbstractRecipe;
import ai.timefold.solver.migration.common.RemoveFieldFromMethodInvocationRecipe;

import org.openrewrite.Recipe;
import org.openrewrite.java.RemoveMethodInvocations;

public class GeneralMethodDeleteInvocationMigrationRecipe extends AbstractRecipe {

    @Override
    public String getDisplayName() {
        return "Remove any calls to methods that no longer exist";
    }

    @Override
    public String getDescription() {
        return "Remove calls to methods that no longer exist.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                // ConstraintStreamImplType
                new RemoveFieldFromMethodInvocationRecipe(
                        "ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig getConstraintStreamImplType()"),
                new RemoveMethodInvocations(
                        "ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig getConstraintStreamImplType()"),
                new RemoveMethodInvocations(
                        "ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig setConstraintStreamImplType(..)"),
                new RemoveMethodInvocations(
                        "ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig withConstraintStreamImplType(..)"),
                new RemoveMethodInvocations(
                        "ai.timefold.solver.test.api.score.stream.ConstraintVerifier withConstraintStreamImplType(..)"),
                // Tabu
                new RemoveFieldFromMethodInvocationRecipe(
                        "ai.timefold.solver.core.config.localsearch.decider.accepter.LocalSearchAcceptorConfig getUndoMoveTabuSize()"),
                new RemoveMethodInvocations(
                        "ai.timefold.solver.core.config.localsearch.decider.accepter.LocalSearchAcceptorConfig getUndoMoveTabuSize()"),
                new RemoveMethodInvocations(
                        "ai.timefold.solver.core.config.localsearch.decider.accepter.LocalSearchAcceptorConfig setUndoMoveTabuSize(..)"),
                new RemoveMethodInvocations(
                        "ai.timefold.solver.core.config.localsearch.decider.accepter.LocalSearchAcceptorConfig withUndoMoveTabuSize(..)"),
                new RemoveFieldFromMethodInvocationRecipe(
                        "ai.timefold.solver.core.config.localsearch.decider.accepter.LocalSearchAcceptorConfig getFadingUndoMoveTabuSize()"),
                new RemoveMethodInvocations(
                        "ai.timefold.solver.core.config.localsearch.decider.accepter.LocalSearchAcceptorConfig getFadingUndoMoveTabuSize()"),
                new RemoveMethodInvocations(
                        "ai.timefold.solver.core.config.localsearch.decider.accepter.LocalSearchAcceptorConfig setFadingUndoMoveTabuSize(..)"),
                new RemoveMethodInvocations(
                        "ai.timefold.solver.core.config.localsearch.decider.accepter.LocalSearchAcceptorConfig withFadingUndoMoveTabuSize(..)"),
                new RemoveFieldFromMethodInvocationRecipe(
                        "ai.timefold.solver.core.config.localsearch.decider.accepter.LocalSearchAcceptorConfig getValueTabuRatio()"),
                new RemoveMethodInvocations(
                        "ai.timefold.solver.core.config.localsearch.decider.accepter.LocalSearchAcceptorConfig getValueTabuRatio()"),
                new RemoveMethodInvocations(
                        "ai.timefold.solver.core.config.localsearch.decider.accepter.LocalSearchAcceptorConfig setValueTabuRatio(..)"),
                new RemoveMethodInvocations(
                        "ai.timefold.solver.core.config.localsearch.decider.accepter.LocalSearchAcceptorConfig withValueTabuRatio(..)"),
                new RemoveFieldFromMethodInvocationRecipe(
                        "ai.timefold.solver.core.config.localsearch.decider.accepter.LocalSearchAcceptorConfig getFadingValueTabuRatio()"),
                new RemoveMethodInvocations(
                        "ai.timefold.solver.core.config.localsearch.decider.accepter.LocalSearchAcceptorConfig getFadingValueTabuRatio()"),
                new RemoveMethodInvocations(
                        "ai.timefold.solver.core.config.localsearch.decider.accepter.LocalSearchAcceptorConfig setFadingValueTabuRatio(..)"),
                new RemoveMethodInvocations(
                        "ai.timefold.solver.core.config.localsearch.decider.accepter.LocalSearchAcceptorConfig withFadingValueTabuRatio(..)"),
                // Drools support
                new RemoveFieldFromMethodInvocationRecipe(
                        "ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig getScoreDrlList()"),
                new RemoveMethodInvocations(
                        "ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig getScoreDrlList()"),
                new RemoveMethodInvocations(
                        "ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig setScoreDrlList(..)"),
                new RemoveMethodInvocations(
                        "ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig withScoreDrlList(..)"),
                // Constraint
                new RemoveFieldFromMethodInvocationRecipe(
                        "ai.timefold.solver.core.api.score.stream.Constraint getConstraintPackage()"),
                new RemoveFieldFromMethodInvocationRecipe(
                        "ai.timefold.solver.core.api.score.stream.Constraint getConstraintId()"),
                new RemoveFieldFromMethodInvocationRecipe(
                        "ai.timefold.solver.core.api.score.stream.Constraint getConstraintFactory()"),
                new RemoveMethodInvocations("ai.timefold.solver.core.api.score.stream.Constraint getConstraintPackage()"),
                new RemoveMethodInvocations("ai.timefold.solver.core.api.score.stream.Constraint getConstraintId()"),
                new RemoveMethodInvocations("ai.timefold.solver.core.api.score.stream.Constraint getConstraintFactory()"),
                // ConstraintFactory
                new RemoveFieldFromMethodInvocationRecipe(
                        "ai.timefold.solver.core.api.score.stream.ConstraintFactory getDefaultConstraintPackage()"),
                new RemoveMethodInvocations(
                        "ai.timefold.solver.core.api.score.stream.ConstraintFactory getDefaultConstraintPackage()"),
                // SolverConfig
                new RemoveMethodInvocations(
                        "ai.timefold.solver.core.config.solver.SolverConfig withConstraintStreamImplType(..)"),
                new RemoveMethodInvocations("ai.timefold.solver.core.config.solver.SolverConfig getDomainAccessType()"),
                new RemoveMethodInvocations("ai.timefold.solver.core.config.solver.SolverConfig setDomainAccessType(..)"),
                new RemoveMethodInvocations("ai.timefold.solver.core.config.solver.SolverConfig withDomainAccessType(..)"),
                new RemoveMethodInvocations("ai.timefold.solver.core.config.solver.SolverConfig determineDomainAccessType()"),
                new RemoveFieldFromMethodInvocationRecipe(
                        "ai.timefold.solver.core.config.solver.SolverConfig getDomainAccessType()"),
                new RemoveFieldFromMethodInvocationRecipe(
                        "ai.timefold.solver.core.config.solver.SolverConfig determineDomainAccessType()"));
    }
}
