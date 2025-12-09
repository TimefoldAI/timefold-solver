package ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.function;

import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.EnumeratingJoiners;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.UniEnumeratingStream;

import org.jspecify.annotations.NullMarked;

/**
 * Created with {@link EnumeratingJoiners}.
 * Used by {@link UniEnumeratingStream#join(Class, BiEnumeratingJoiner[])}, ...
 *
 * @see Joiners
 */
@NullMarked
public interface BiEnumeratingJoiner<A, B> {

    BiEnumeratingJoiner<A, B> and(BiEnumeratingJoiner<A, B> otherJoiner);

}
