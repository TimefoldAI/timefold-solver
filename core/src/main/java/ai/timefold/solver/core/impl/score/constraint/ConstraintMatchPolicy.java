package ai.timefold.solver.core.impl.score.constraint;

import ai.timefold.solver.core.api.solver.ScoreAnalysisFetchPolicy;

import org.jspecify.annotations.NullMarked;

/**
 * Determines whether constraint match is enabled and whether constraint match justification is enabled.
 *
 * @see ai.timefold.solver.core.api.score.constraint.ConstraintMatch
 * @see ai.timefold.solver.core.api.score.stream.ConstraintJustification
 */
@NullMarked
public enum ConstraintMatchPolicy {

    DISABLED(false, false),
    ENABLED_WITHOUT_JUSTIFICATIONS(true, false),
    ENABLED(true, true);

    /**
     * To achieve the most performance out of the underlying solver,
     * the policy should match whatever policy was used for score analysis.
     * For example, if the fetch policy specifies that only match counts are necessary and not matches themselves
     * ({@link ScoreAnalysisFetchPolicy#FETCH_MATCH_COUNT}),
     * we can configure the solver to not produce justifications ({@link #ENABLED_WITHOUT_JUSTIFICATIONS}).
     *
     * @param scoreAnalysisFetchPolicy
     * @return Match policy best suited for the given fetch policy.
     */
    public static ConstraintMatchPolicy match(ScoreAnalysisFetchPolicy scoreAnalysisFetchPolicy) {
        return switch (scoreAnalysisFetchPolicy) {
            case FETCH_MATCH_COUNT, FETCH_SHALLOW -> ENABLED_WITHOUT_JUSTIFICATIONS;
            case FETCH_ALL -> ENABLED;
        };
    }

    private final boolean enabled;
    private final boolean justificationEnabled;

    ConstraintMatchPolicy(boolean enabled, boolean justificationEnabled) {
        this.enabled = enabled;
        this.justificationEnabled = justificationEnabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isJustificationEnabled() {
        return justificationEnabled;
    }

}
