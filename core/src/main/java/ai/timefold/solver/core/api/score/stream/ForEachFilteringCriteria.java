package ai.timefold.solver.core.api.score.stream;

/**
 * In constraint streams, {@link ConstraintFactory#forEach(Class)}
 * will filter inconsistent and unassigned entities by default.
 * In order to include these entities,
 * {@link ConstraintFactory#forEachIncluding(Class, ForEachFilteringCriteria...)} must
 * be used instead.
 */
public enum ForEachFilteringCriteria {
    INCLUDE_UNASSIGNED,
    INCLUDE_INCONSISTENT
}
