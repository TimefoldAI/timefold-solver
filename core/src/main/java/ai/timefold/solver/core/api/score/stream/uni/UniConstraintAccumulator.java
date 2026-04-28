package ai.timefold.solver.core.api.score.stream.uni;

import java.util.function.BiFunction;

public interface UniConstraintAccumulator<ResultContainer_, A>
        extends BiFunction<ResultContainer_, A, Runnable> {

    @Override
    Runnable apply(ResultContainer_ resultContainer, A a);

}
