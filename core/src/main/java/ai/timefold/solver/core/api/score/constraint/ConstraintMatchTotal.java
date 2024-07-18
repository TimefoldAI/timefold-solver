package ai.timefold.solver.core.api.score.constraint;

import java.util.Set;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.ScoreExplanation;
import ai.timefold.solver.core.api.solver.SolutionManager;

/**
 * Explains the {@link Score} of a {@link PlanningSolution}, from the opposite side than {@link Indictment}.
 * Retrievable from {@link ScoreExplanation#getConstraintMatchTotalMap()}.
 *
 * <p>
 * If possible, prefer using {@link SolutionManager#analyze(Object)} instead.
 *
 * @param <Score_> the actual score type
 */
public interface ConstraintMatchTotal<Score_ extends Score<Score_>> {

    /**
     * @param constraintPackage never null
     * @param constraintName never null
     * @return never null
     * @deprecated Prefer {@link ConstraintRef#of(String, String)}.
     */
    @Deprecated(forRemoval = true, since = "1.4.0")
    static String composeConstraintId(String constraintPackage, String constraintName) {
        return constraintPackage + "/" + constraintName;
    }

    /**
     * @return never null
     */
    ConstraintRef getConstraintRef();

    /**
     * @return never null
     * @deprecated Prefer {@link #getConstraintRef()}.
     */
    @Deprecated(forRemoval = true, since = "1.4.0")
    default String getConstraintPackage() {
        return getConstraintRef().packageName();
    }

    /**
     * @return never null
     * @deprecated Prefer {@link #getConstraintRef()}.
     */
    @Deprecated(forRemoval = true, since = "1.4.0")
    default String getConstraintName() {
        return getConstraintRef().constraintName();
    }

    /**
     * The effective value of constraint weight after applying optional overrides.
     * It is independent to the state of the {@link PlanningVariable planning variables}.
     * Do not confuse with {@link #getScore()}.
     *
     * @return never null
     */
    Score_ getConstraintWeight();

    /**
     * @return never null
     */
    Set<ConstraintMatch<Score_>> getConstraintMatchSet();

    /**
     * @return {@code >= 0}
     */
    default int getConstraintMatchCount() {
        return getConstraintMatchSet().size();
    }

    /**
     * Sum of the {@link #getConstraintMatchSet()}'s {@link ConstraintMatch#getScore()}.
     *
     * @return never null
     */
    Score_ getScore();

    /**
     * @return never null
     * @deprecated Prefer {@link #getConstraintRef()}.
     */
    @Deprecated(forRemoval = true, since = "1.4.0")
    default String getConstraintId() {
        return getConstraintRef().constraintId();
    }

}
