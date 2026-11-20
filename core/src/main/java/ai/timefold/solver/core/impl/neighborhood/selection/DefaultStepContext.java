package ai.timefold.solver.core.impl.neighborhood.selection;

import ai.timefold.solver.core.preview.api.neighborhood.selection.StepContext;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record DefaultStepContext<Solution_>(Solution_ solution) implements StepContext<Solution_> {
}
