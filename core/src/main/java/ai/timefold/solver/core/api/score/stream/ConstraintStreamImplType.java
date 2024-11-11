package ai.timefold.solver.core.api.score.stream;

/**
 * The type of {@link ConstraintStream} implementation.
 * 
 * @deprecated There is only one implementation.
 */
@Deprecated(forRemoval = true, since = "1.16.0")
public enum ConstraintStreamImplType {
    BAVET,
    /**
     * @deprecated in favor of {@link #BAVET}.
     */
    @Deprecated(forRemoval = true)
    DROOLS
}
