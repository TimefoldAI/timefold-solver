package ai.timefold.solver.core.impl.move.streams.maybeapi;

import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.UniDataStream;

import org.jspecify.annotations.NullMarked;

/**
 * Created with {@link Joiners}.
 * Used by {@link UniDataStream#join(Class, BiDataJoiner[])}, ...
 *
 * @see Joiners
 */
@NullMarked
public interface BiDataJoiner<A, B> {

    BiDataJoiner<A, B> and(BiDataJoiner<A, B> otherJoiner);

}
