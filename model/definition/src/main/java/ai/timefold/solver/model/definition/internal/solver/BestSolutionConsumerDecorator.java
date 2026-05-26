package ai.timefold.solver.model.definition.internal.solver;

import java.util.function.Consumer;

import ai.timefold.solver.core.api.solver.event.NewBestSolutionEvent;

public interface BestSolutionConsumerDecorator {

    <Solution_> Consumer<NewBestSolutionEvent<Solution_>>
            decorate(Consumer<NewBestSolutionEvent<Solution_>> bestSolutionEventConsumer);
}
