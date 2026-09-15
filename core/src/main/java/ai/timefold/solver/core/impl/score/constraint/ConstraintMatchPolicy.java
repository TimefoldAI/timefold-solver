package ai.timefold.solver.core.impl.score.constraint;

import ai.timefold.solver.core.api.solver.ScoreAnalysisFetchPolicy;

import org.jspecify.annotations.NullMarked;

/**
 * Determines whether constraint match is enabled and whether constraint match justification is enabled.
 *
 * @see ConstraintMatch
 * @see ai.timefold.solver.core.api.score.stream.ConstraintJustification
 */
@NullMarked
public enum ConstraintMatchPolicy {

    DISABLED(false, false, false),
    ENABLED_WITHOUT_JUSTIFICATIONS_AND_INDICTMENTS(true, false, false),
    ENABLED_WITHOUT_JUSTIFICATIONS(true, false, true),
    ENABLED_WITHOUT_INDICTMENTS(true, true, false),
    ENABLED(true, true, true);

    /**
     * To achieve the most performance out of the underlying solver,
     * the policy should match whatever policy was used for score analysis.
     * For example, if the fetch policy specifies that only match counts are necessary and not matches themselves
     * ({@link ScoreAnalysisFetchPolicy#FETCH_MATCH_COUNT}),
     * we can configure the solver to not produce justifications ({@link #ENABLED_WITHOUT_JUSTIFICATIONS_AND_INDICTMENTS}).
     *
     * @param scoreAnalysisFetchPolicy
     * @return Match policy best suited for the given fetch policy.
     */
    public static ConstraintMatchPolicy match(ScoreAnalysisFetchPolicy scoreAnalysisFetchPolicy) {
        return switch (scoreAnalysisFetchPolicy) {
            case FETCH_MATCH_COUNT, FETCH_SHALLOW -> ENABLED_WITHOUT_JUSTIFICATIONS_AND_INDICTMENTS;
            case FETCH_ALL -> ENABLED;
        };
    }

    private final boolean enabled;
    private final boolean justificationEnabled;
    private final boolean indictmentsEnabled;

    ConstraintMatchPolicy(boolean enabled, boolean justificationEnabled, boolean indictmentsEnabled) {
        this.enabled = enabled;
        this.justificationEnabled = justificationEnabled;
        this.indictmentsEnabled = indictmentsEnabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isJustificationEnabled() {
        return justificationEnabled;
    }

    public boolean isIndictmentsEnabled() {
        return indictmentsEnabled;
    }
}
