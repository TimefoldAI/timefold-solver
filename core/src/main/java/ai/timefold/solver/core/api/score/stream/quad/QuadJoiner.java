package ai.timefold.solver.core.api.score.stream.quad;

import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintStream;

import org.jspecify.annotations.NonNull;

/**
 * Created with {@link Joiners}.
 * Used by {@link TriConstraintStream#join(Class, QuadJoiner)}, ...
 *
 * @see Joiners
 */
public interface QuadJoiner<A, B, C, D> {

    @NonNull
    QuadJoiner<A, B, C, D> and(@NonNull QuadJoiner<A, B, C, D> otherJoiner);

}
