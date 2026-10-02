package ai.timefold.solver.core.preview.api.neighborhood.stream.dataset;

import ai.timefold.solver.core.preview.api.neighborhood.MoveIteratorSession;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.BiEnumeratingStream;

import org.jspecify.annotations.NullMarked;

/**
 * An opaque handle to a dataset created via {@link BiEnumeratingStream#asCachedDataset()}
 * or via
 * {@link UniDataset#join(ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.UniEnumeratingStream)}.
 * Carries no behavior of its own; resolve it against a {@link MoveIteratorSession}
 * to obtain a {@link BiDatasetInstance}.
 *
 * @param <Solution_> the solution type
 * @param <A> the type of the dataset's left rows
 * @param <B> the type of the dataset's right rows
 */
@NullMarked
public interface BiDataset<Solution_, A, B> {

}
