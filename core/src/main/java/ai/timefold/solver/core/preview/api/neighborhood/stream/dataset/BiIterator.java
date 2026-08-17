package ai.timefold.solver.core.preview.api.neighborhood.stream.dataset;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A cursor over the rows of a {@link BiDatasetInstance}. No pair object is ever allocated;
 * call {@link #next()} to advance, then read the current row via {@link #getA()}/{@link #getB()}.
 *
 * @param <A> the type of the left row
 * @param <B> the type of the right row
 */
@NullMarked
public interface BiIterator<A, B> {

    boolean hasNext();

    void next();

    @Nullable
    A getA();

    @Nullable
    B getB();

}
