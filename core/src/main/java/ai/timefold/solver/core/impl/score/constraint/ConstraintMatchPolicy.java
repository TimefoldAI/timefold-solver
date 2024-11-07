package ai.timefold.solver.core.impl.score.constraint;

import ai.timefold.solver.core.api.solver.ScoreAnalysisFetchPolicy;

public enum ConstraintMatchPolicy {

    DISABLED(false, false),
    ENABLED_WITHOUT_JUSTIFICATIONS(true, false),
    ENABLED(true, true);

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
