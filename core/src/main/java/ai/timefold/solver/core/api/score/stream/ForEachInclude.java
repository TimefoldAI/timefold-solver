package ai.timefold.solver.core.api.score.stream;

/**
 * In constraint streams, {@link ConstraintFactory#forEach(Class)}
 * will filter inconsistent and unassigned entities by default.
 * In order to select the filtered entities,
 * {@link ConstraintFactory#forEachIncluding(Class, ForEachInclude...)} must
 * be used instead.
 */
public enum ForEachInclude {
    UNASSIGNED,
    INCONSISTENT
}
