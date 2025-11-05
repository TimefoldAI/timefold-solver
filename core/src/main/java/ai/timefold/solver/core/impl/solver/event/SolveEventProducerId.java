package ai.timefold.solver.core.impl.solver.event;

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
     * The cause is unknown. This is the {@link EventProducerId}
     * used when one of the deprecated {@link BestSolutionChangedEvent}
     * constructors are used.
     * 
     * @deprecated Only used when Users manually construct instances of {@link BestSolutionChangedEvent}.
     */
    @Deprecated(forRemoval = true, since = "1.28.0")
    UNKNOWN("Unknown"),

    /**
     * The solver was started with an initialized solution.
     */
    SOLVING_STARTED("Solving started"),

    /**
     * One or more problem changes occured that change the best solution.
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
}
