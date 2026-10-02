package ai.timefold.solver.core.preview.api.neighborhood.stream.dataset;

import java.util.Iterator;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A cursor over the rows of a {@link BiDatasetInstance}.
 * No pair object is ever allocated;
 * call {@link #next()} to advance,
 * then read the current row via {@link #a()}/{@link #b()}.
 * <p>
 * In all other aspects,
 * behaves as {@link Iterator}.
 *
 * @param <A> the type of the left row
 * @param <B> the type of the right row
 */
@NullMarked
public interface BiIterator<A, B> {

    boolean hasNext();

    void next();

    @Nullable
    A a();

    @Nullable
    B b();

}
