package ai.timefold.solver.core.impl.solver.event;

import java.util.OptionalInt;

import ai.timefold.solver.core.api.solver.event.BestSolutionChangedEvent;
import ai.timefold.solver.core.api.solver.event.EventProducerId;

import org.jspecify.annotations.NullMarked;

/**
 * {@link EventProducerId} for when a {@link BestSolutionChangedEvent} is not
 * caused by a phase.
 */
@NullMarked
public enum SolveEventProducerId implements EventProducerId {

    /**
     * The solver was started with an initialized solution.
     */
    SOLVING_STARTED("Solving started"),

    /**
     * One or more problem changes occurred that change the best solution.
     */
    PROBLEM_CHANGE("Problem change");

    private final String producerId;

    SolveEventProducerId(String producerId) {
        this.producerId = producerId;
    }

    @Override
    public String producerId() {
        return producerId;
    }

    @Override
    public String simpleProducerName() {
        return producerId;
    }

    @Override
    public OptionalInt phaseIndex() {
        return OptionalInt.empty();
    }
}
