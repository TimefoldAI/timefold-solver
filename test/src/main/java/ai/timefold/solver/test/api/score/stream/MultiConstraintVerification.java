package ai.timefold.solver.test.api.score.stream;

import org.jspecify.annotations.NonNull;

public interface MultiConstraintVerification<Solution_> {

    /**
     * As defined by {@link SingleConstraintVerification#given(Object...)}.
     *
     * @param facts at least one
     */
    @NonNull
    MultiConstraintAssertion given(@NonNull Object @NonNull... facts);

    /**
     * As defined by {@link SingleConstraintVerification#givenSolution(Object)}.
     */
    @NonNull
    ShadowVariableAwareMultiConstraintAssertion givenSolution(@NonNull Solution_ solution);

}
