package ai.timefold.solver.core.impl.solver;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.ScoreExplanation;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.solver.RecommendedAssignment;
import ai.timefold.solver.core.api.solver.RecommendedFit;
import ai.timefold.solver.core.api.solver.ScoreAnalysisFetchPolicy;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolutionUpdatePolicy;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.impl.score.DefaultScoreExplanation;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.stream.BavetConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.core.impl.util.MutableReference;
import ai.timefold.solver.core.preview.api.domain.solution.diff.PlanningSolutionDiff;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public final class DefaultSolutionManager<Solution_, Score_ extends Score<Score_>>
        implements SolutionManager<Solution_, Score_> {

    private final DefaultSolverFactory<Solution_> solverFactory;
    private final ScoreDirectorFactory<Solution_, Score_> scoreDirectorFactory;

    public <ProblemId_> DefaultSolutionManager(SolverManager<Solution_, ProblemId_> solverManager) {
        this(((DefaultSolverManager<Solution_, ProblemId_>) solverManager).getSolverFactory());
    }

    public DefaultSolutionManager(SolverFactory<Solution_> solverFactory) {
        this.solverFactory = ((DefaultSolverFactory<Solution_>) solverFactory);
        this.scoreDirectorFactory = this.solverFactory.getScoreDirectorFactory();
    }

    public ScoreDirectorFactory<Solution_, Score_> getScoreDirectorFactory() {
        return scoreDirectorFactory;
    }

    @Override
    public @Nullable Score_ update(@NonNull Solution_ solution, @NonNull SolutionUpdatePolicy solutionUpdatePolicy) {
        if (solutionUpdatePolicy == SolutionUpdatePolicy.NO_UPDATE) {
            throw new IllegalArgumentException("Can not call " + this.getClass().getSimpleName()
                    + ".update() with this solutionUpdatePolicy (" + solutionUpdatePolicy + ").");
        }
        return callScoreDirector(solution, solutionUpdatePolicy,
                s -> s.getSolutionDescriptor().getScore(s.getWorkingSolution()), ConstraintMatchPolicy.DISABLED, false);
    }

    private <Result_> Result_ callScoreDirector(Solution_ solution,
            SolutionUpdatePolicy solutionUpdatePolicy, Function<InnerScoreDirector<Solution_, Score_>, Result_> function,
            ConstraintMatchPolicy constraintMatchPolicy, boolean cloneSolution) {
        var isShadowVariableUpdateEnabled = solutionUpdatePolicy.isShadowVariableUpdateEnabled();
        var nonNullSolution = Objects.requireNonNull(solution);
        try (var scoreDirector = getScoreDirectorFactory().createScoreDirectorBuilder()
                .withLookUpEnabled(cloneSolution)
                .withConstraintMatchPolicy(constraintMatchPolicy)
                .withExpectShadowVariablesInCorrectState(!isShadowVariableUpdateEnabled)
                .build()) {
            nonNullSolution = cloneSolution ? scoreDirector.cloneSolution(nonNullSolution) : nonNullSolution;
            scoreDirector.setWorkingSolution(nonNullSolution);
            if (constraintMatchPolicy.isEnabled() && !scoreDirector.getConstraintMatchPolicy().isEnabled()) {
                throw new IllegalStateException("""
                        Requested constraint matching but score director doesn't support it.
                        Maybe use Constraint Streams instead of Easy or Incremental score calculator?""");
            }
            if (isShadowVariableUpdateEnabled) {
                scoreDirector.forceTriggerVariableListeners();
            }
            if (solutionUpdatePolicy.isScoreUpdateEnabled()) {
                scoreDirector.calculateScore();
            }
            return function.apply(scoreDirector);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NonNull ScoreExplanation<Solution_, Score_> explain(@NonNull Solution_ solution,
            @NonNull SolutionUpdatePolicy solutionUpdatePolicy) {
        var currentScore = (Score_) scoreDirectorFactory.getSolutionDescriptor().getScore(solution);
        var explanation = callScoreDirector(solution, solutionUpdatePolicy, DefaultScoreExplanation::new,
                ConstraintMatchPolicy.ENABLED, false);
        assertFreshScore(solution, currentScore, explanation.getScore(), solutionUpdatePolicy);
        return explanation;
    }

    private void assertFreshScore(Solution_ solution, Score_ currentScore, Score_ calculatedScore,
            SolutionUpdatePolicy solutionUpdatePolicy) {
        if (!solutionUpdatePolicy.isScoreUpdateEnabled() && currentScore != null) {
            // Score update is not enabled and score is not null; this means the score is supposed to be valid.
            // Yet it is different from a freshly calculated score, suggesting previous score corruption.
            if (!calculatedScore.equals(currentScore)) {
                throw new IllegalStateException("""
                        Current score (%s) and freshly calculated score (%s) for solution (%s) do not match.
                        Maybe run %s environment mode to check for score corruptions.
                        Otherwise enable %s.%s to update the stale score.
                        """
                        .formatted(currentScore, calculatedScore, solution, EnvironmentMode.TRACKED_FULL_ASSERT,
                                SolutionUpdatePolicy.class.getSimpleName(),
                                SolutionUpdatePolicy.UPDATE_ALL));
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NonNull ScoreAnalysis<Score_> analyze(@NonNull Solution_ solution, @NonNull ScoreAnalysisFetchPolicy fetchPolicy,
            @NonNull SolutionUpdatePolicy solutionUpdatePolicy) {
        Objects.requireNonNull(fetchPolicy, "fetchPolicy");
        var currentScore = (Score_) scoreDirectorFactory.getSolutionDescriptor().getScore(solution);
        var analysis = callScoreDirector(solution, solutionUpdatePolicy,
                scoreDirector -> scoreDirector.buildScoreAnalysis(fetchPolicy), ConstraintMatchPolicy.match(fetchPolicy),
                false);
        assertFreshScore(solution, currentScore, analysis.score(), solutionUpdatePolicy);
        return analysis;
    }

    @Override
    public @NonNull PlanningSolutionDiff<Solution_> diff(@NonNull Solution_ oldSolution, @NonNull Solution_ newSolution) {
        solverFactory.ensurePreviewFeature(PreviewFeature.PLANNING_SOLUTION_DIFF);
        return solverFactory.getSolutionDescriptor()
                .diff(oldSolution, newSolution);
    }

    @Override
    public @NonNull <In_, Out_> List<RecommendedAssignment<Out_, Score_>> recommendAssignment(@NonNull Solution_ solution,
            @NonNull In_ evaluatedEntityOrElement, @NonNull Function<In_, Out_> propositionFunction,
            @NonNull ScoreAnalysisFetchPolicy fetchPolicy) {
        var assigner = new Assigner<Solution_, Score_, RecommendedAssignment<Out_, Score_>, In_, Out_>(solverFactory,
                propositionFunction, DefaultRecommendedAssignment::new, fetchPolicy, solution, evaluatedEntityOrElement);
        return callScoreDirector(solution, SolutionUpdatePolicy.UPDATE_ALL, assigner, ConstraintMatchPolicy.match(fetchPolicy),
                true);
    }

    @Override
    public <In_, Out_> List<RecommendedFit<Out_, Score_>> recommendFit(Solution_ solution, In_ fittedEntityOrElement,
            Function<In_, Out_> propositionFunction, ScoreAnalysisFetchPolicy fetchPolicy) {
        var assigner = new Assigner<Solution_, Score_, RecommendedFit<Out_, Score_>, In_, Out_>(solverFactory,
                propositionFunction, DefaultRecommendedFit::new, fetchPolicy, solution, fittedEntityOrElement);
        return callScoreDirector(solution, SolutionUpdatePolicy.UPDATE_ALL, assigner, ConstraintMatchPolicy.match(fetchPolicy),
                true);
    }

    /**
     * Generates a Bavet node network visualization for the given solution.
     * It uses a Graphviz DOT language representation.
     * The string returned by this method can be converted to an image using {@code dot}:
     *
     * <pre>
     * $ dot -Tsvg input.dot > output.svg
     * </pre>
     * 
     * This assumes the string returned by this method is saved to a file named {@code input.dot}.
     * 
     * <p>
     * The node network itself is an internal implementation detail of Constraint Streams.
     * Do not rely on any particular node network structure in production code,
     * and do not micro-optimize your constraints to match the node network.
     * Such optimizations are destined to become obsolete and possibly harmful as the node network evolves.
     * 
     * <p>
     * This method is only provided for debugging purposes
     * and is deliberately not part of the public API.
     * Its signature or behavior may change without notice,
     * and it may be removed in future versions.
     *
     * @see <a href="https://graphviz.org/doc/info/lang.html">Graphviz DOT language</a>
     *
     * @param solution Will be used to read constraint weights, which determine the final node network.
     * @return A string representing the node network in Graphviz DOT language.
     */
    public @NonNull String visualizeNodeNetwork(@NonNull Solution_ solution) {
        if (scoreDirectorFactory instanceof BavetConstraintStreamScoreDirectorFactory<Solution_, ?> bavetScoreDirectorFactory) {
            var result = new MutableReference<String>(null);
            bavetScoreDirectorFactory.newSession(solution, ConstraintMatchPolicy.ENABLED, false, result::setValue);
            return result.getValue();
        }
        throw new UnsupportedOperationException("Node network visualization is only supported when using Constraint Streams.");
    }

}
