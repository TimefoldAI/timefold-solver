package ai.timefold.solver.core.api.score.stream.bi;

import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;

import org.jspecify.annotations.NonNull;

/**
 * Created with {@link Joiners}.
 * Used by {@link UniConstraintStream#join(Class, BiJoiner)}, ...
 *
 * @see Joiners
 */
public interface BiJoiner<A, B> {
    @NonNull
    BiJoiner<A, B> and(@NonNull BiJoiner<A, B> otherJoiner);

}
