package ai.timefold.solver.core.impl.move.streams.maybeapi.stream.pickers;

import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.UniDataStream;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.UniMoveStream;

import org.jspecify.annotations.NullMarked;

/**
 * Created with {@link Joiners}.
 * Used by {@link UniMoveStream#pick(UniDataStream, BiPicker[])} , ...
 *
 * @see Joiners
 */
@NullMarked
public interface BiPicker<A, B> {

    BiPicker<A, B> and(BiPicker<A, B> otherPicker);

}
