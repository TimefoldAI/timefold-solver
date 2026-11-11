package ai.timefold.solver.core.impl.neighborhood.selection;

import java.util.List;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.preview.api.neighborhood.selection.MoveSource;
import ai.timefold.solver.core.preview.api.neighborhood.selection.PhaseContext;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record DefaultPhaseContext<Solution_>(List<MoveSource<Solution_>> moveSources, Solution_ solution,
        RandomGenerator random)
        implements
            PhaseContext<Solution_> {
}
