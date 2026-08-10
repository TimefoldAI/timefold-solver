package ai.timefold.solver.core.preview.api.neighborhood.stream.dataset;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A cursor over the rows of a {@link BiDatasetInstance}. No pair object is ever allocated;
 * call {@link #next()} to advance, then read the current row via {@link #getA()}/{@link #getB()}.
 * <p>
 * <strong>This package and all of its contents are part of the Neighborhoods API,
 * which is under development and is only offered as a preview feature.</strong>
 * There are no guarantees for backward compatibility;
 * any class, method, or field may change or be removed without prior notice,
 * although we will strive to avoid this as much as possible.
 * <p>
 * We encourage you to try the API and give us feedback on your experience with it,
 * before we finalize the API.
 * Please direct your feedback to
 * <a href="https://github.com/TimefoldAI/timefold-solver/discussions">Timefold Solver GitHub</a>
 * or to <a href="https://discord.com/channels/1413420192213631086/1414521616955605003">Timefold Discord</a>.
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
