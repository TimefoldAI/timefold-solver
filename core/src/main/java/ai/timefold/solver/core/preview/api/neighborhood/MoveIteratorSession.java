package ai.timefold.solver.core.preview.api.neighborhood;

import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.BiDataset;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.BiDatasetInstance;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.UniDataset;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.UniDatasetInstance;

import org.jspecify.annotations.NullMarked;

/**
 * The runtime handle a {@link MoveIteratorProvider} resolves its cached datasets and solution state against.
 *
 * @param <Solution_> the solution type
 */
@NullMarked
public interface MoveIteratorSession<Solution_> {

    SolutionView<Solution_> getSolutionView();

    <A> UniDatasetInstance<A> getInstance(UniDataset<Solution_, A> dataset);

    <A, B> BiDatasetInstance<A, B> getInstance(BiDataset<Solution_, A, B> dataset);

}
