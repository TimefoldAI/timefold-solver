package ai.timefold.solver.core.api.solver.event;

import java.util.OptionalInt;

import ai.timefold.solver.core.config.solver.SolverConfig;

import org.jspecify.annotations.NullMarked;

/**
 * Identifies the producer of a {@link BestSolutionChangedEvent}.
 * Will be an instance of {@link PhaseEventProducerId} if the event is associated
 * with a phase, or a {@link SolveEventProducerId} otherwise.
 */
@NullMarked
public sealed interface EventProducerId permits PhaseEventProducerId, SolveEventProducerId {
    /**
     * An unique string identifying what produced the event, either of the form
     * "Event" where "Event" is a string describing the event that cause the update (like "Solving started")
     * or "Phase (index)", where "Phase" is a string identifying the type of phase (like "Construction Heuristics")
     * and index is the index of the phase in the {@link SolverConfig#getPhaseConfigList()}.
     * 
     * @return An unique string identifying what produced the event.
     */
    String producerId();

    /**
     * A (non-unique) string describing what produced the event.
     * Events from different phases of the same type (for example,
     * when multiple Construction Heuristics are configured)
     * will return the same value.
     * 
     * @return A (non-unique) string describing what produced the event.
     */
    String simpleProducerName();

    /**
     * An optional integer, that if present, identify what index in {@link SolverConfig#getPhaseConfigList()}
     * corresponds to the Phase that produced the event.
     *
     * @return The index of the Phase that produced the event, or {@link OptionalInt#empty()} if the event producer
     *         is not associated with a phase.
     */
    OptionalInt eventPhaseIndex();

    static EventProducerId unknown() {
        return SolveEventProducerId.UNKNOWN;
    }

    static EventProducerId solvingStarted() {
        return SolveEventProducerId.SOLVING_STARTED;
    }

    static EventProducerId problemChange() {
        return SolveEventProducerId.PROBLEM_CHANGE;
    }

    @Deprecated(forRemoval = true, since = "1.28.0")
    static EventProducerId noChange(int phaseIndex) {
        return new PhaseEventProducerId(PhaseType.NO_CHANGE, phaseIndex);
    }

    static EventProducerId constructionHeuristic(int phaseIndex) {
        return new PhaseEventProducerId(PhaseType.CONSTRUCTION_HEURISTIC, phaseIndex);
    }

    static EventProducerId localSearch(int phaseIndex) {
        return new PhaseEventProducerId(PhaseType.LOCAL_SEARCH, phaseIndex);
    }

    static EventProducerId exhaustiveSearch(int phaseIndex) {
        return new PhaseEventProducerId(PhaseType.EXHAUSTIVE_SEARCH, phaseIndex);
    }

    static EventProducerId partitionedSearch(int phaseIndex) {
        return new PhaseEventProducerId(PhaseType.PARTITIONED_SEARCH, phaseIndex);
    }

    static EventProducerId customPhase(int phaseIndex) {
        return new PhaseEventProducerId(PhaseType.CUSTOM_PHASE, phaseIndex);
    }
}
