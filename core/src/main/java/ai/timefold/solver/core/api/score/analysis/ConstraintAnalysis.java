package ai.timefold.solver.core.api.score.analysis;

import java.util.HashSet;
import java.util.List;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintRef;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Note: {@link ScoreAnalysis} is exclusive to Timefold Solver Enterprise Edition.
 * 
 * @see ScoreAnalysis Description of score analysis and the purpose of this class.
 */
@NullMarked
public interface ConstraintAnalysis<Score_ extends Score<Score_>> {

    ConstraintRef constraintRef();

    Score_ weight();

    Score_ score();

    /**
     *
     * @return null if analysis not available;
     *         empty if constraint has no matches, but still non-zero constraint weight;
     *         non-empty if constraint has matches.
     *         This is a {@link List} to simplify access to individual elements,
     *         but it contains no duplicates just like {@link HashSet} wouldn't.
     */
    @Nullable
    List<MatchAnalysis<Score_>> matches();

    /**
     * @return
     *         <ul>
     *         <li>For regular constraint analysis:
     *         -1 if analysis not available,
     *         0 if constraint has no matches,
     *         positive if constraint has matches.
     *         Equal to the size of the {@link #matches} list.</li>
     *         <li>For a {@link ScoreAnalysis#diff(ScoreAnalysis) diff of constraint analyses}:
     *         positive if the constraint has more matches in the new analysis,
     *         zero if the number of matches is the same in both,
     *         negative otherwise.
     *         Need not be equal to the size of the {@link #matches} list.</li>
     *         </ul>
     */
    int matchCount();

    /**
     * Return id of the constraint that this analysis is for.
     *
     * @return equal to {@code constraintRef.id()}
     */
    default String constraintId() {
        return constraintRef().id();
    }

    /**
     * Returns a diagnostic text that explains part of the score quality through the {@link ConstraintAnalysis} API.
     * The summary will be limited to the first 3 matches per constraint;
     * see {@link #summarize(int)} to specify a custom limit.
     * The summary is built fresh every time the method is called.
     */
    String summarize();

    /**
     * Return a diagnostic text that explains part of the score quality as defined by {@link #summarize()},
     * but allows specifying the maximum number of constraint matches to display per constraint.
     *
     * @param topLimit maximum number of constraint matches to display per constraint
     *        Use {@link Integer#MAX_VALUE} to show all matches.
     */
    String summarize(int topLimit);

}
