package ai.timefold.solver.core.api.score.constraint;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.ScoreExplanation;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;

import org.jspecify.annotations.NullMarked;

/**
 * Explains the {@link Score} of a {@link PlanningSolution}, from the opposite side than {@link ConstraintMatchTotal}.
 * Retrievable from {@link ScoreExplanation#getIndictmentMap()}.
 *
 * @param <Score_> the actual score type
 */
@NullMarked
public interface Indictment<Score_ extends Score<Score_>> {

    /**
     * The object that was involved in causing the constraints to match.
     * It is part of {@link ConstraintMatch#getIndictedObjectList()} of every {@link ConstraintMatch}
     * returned by {@link #getConstraintMatchSet()}.
     *
     * @param <IndictedObject_> Shorthand so that the user does not need to cast in user code.
     */
    <IndictedObject_> IndictedObject_ getIndictedObject();

    Set<ConstraintMatch<Score_>> getConstraintMatchSet();

    /**
     * @return {@code >= 0}
     */
    default int getConstraintMatchCount() {
        return getConstraintMatchSet().size();
    }

    /**
     * Retrieve {@link ConstraintJustification} instances associated with {@link ConstraintMatch}es in
     * {@link #getConstraintMatchSet()}.
     * This is equivalent to retrieving {@link #getConstraintMatchSet()}
     * and collecting all {@link ConstraintMatch#getJustification()} objects into a list.
     *
     * @return guaranteed to contain unique instances
     */
    List<ConstraintJustification> getJustificationList();

    /**
     * Retrieve {@link ConstraintJustification} instances associated with {@link ConstraintMatch}es in
     * {@link #getConstraintMatchSet()}, which are of (or extend) a given constraint constraintverifier implementation.
     * This is equivalent to retrieving {@link #getConstraintMatchSet()}
     * and collecting all matching {@link ConstraintMatch#getJustification()} objects into a list.
     *
     * @return guaranteed to contain unique instances
     */
    default <ConstraintJustification_ extends ConstraintJustification> List<ConstraintJustification_>
            getJustificationList(Class<ConstraintJustification_> justificationClass) {
        return getJustificationList()
                .stream()
                .filter(justification -> justificationClass.isAssignableFrom(justification.getClass()))
                .map(j -> (ConstraintJustification_) j)
                .collect(Collectors.toList());
    }

    /**
     * Sum of the {@link #getConstraintMatchSet()}'s {@link ConstraintMatch#getScore()}.
     */
    Score_ getScore();

}
