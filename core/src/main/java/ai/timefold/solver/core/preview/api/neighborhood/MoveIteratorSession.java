package ai.timefold.solver.core.preview.api.neighborhood;

import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.BiDataset;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.BiDatasetInstance;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.UniDataset;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.UniDatasetInstance;

import org.jspecify.annotations.NullMarked;

/**
 * The runtime handle a {@link MoveIteratorProvider} resolves its registered datasets and solution state against.
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
 * @param <Solution_> the solution type
 */
@NullMarked
public interface MoveIteratorSession<Solution_> {

    SolutionView<Solution_> getSolutionView();

    <A> UniDatasetInstance<A> getInstance(UniDataset<Solution_, A> dataset);

    <A, B> BiDatasetInstance<A, B> getInstance(BiDataset<Solution_, A, B> dataset);

}
