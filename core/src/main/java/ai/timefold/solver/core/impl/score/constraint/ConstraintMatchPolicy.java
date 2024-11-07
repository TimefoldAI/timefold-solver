package ai.timefold.solver.core.impl.score.constraint;

public enum ConstraintMatchPolicy {

    DISABLED(false, false),
    ENABLED_WITHOUT_JUSTIFICATIONS(true, false),
    ENABLED(true, true);

    private final boolean enabled;
    private final boolean justificationsEnabled;

    ConstraintMatchPolicy(boolean enabled, boolean justificationsEnabled) {
        this.enabled = enabled;
        this.justificationsEnabled = justificationsEnabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isJustificationEnabled() {
        return justificationsEnabled;
    }

}
