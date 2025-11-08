package ai.timefold.solver.core.api.solver.event;

import java.util.OptionalInt;

import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.impl.phase.NoChangePhase;
import ai.timefold.solver.core.impl.phase.PhaseType;
import ai.timefold.solver.core.impl.phase.event.PhaseEventProducerId;
import ai.timefold.solver.core.impl.solver.event.SolveEventProducerId;

import org.jspecify.annotations.NullMarked;

/**
 * Identifies the producer of a {@link BestSolutionChangedEvent}.
 */
@NullMarked
public interface EventProducerId {
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
     * If present, the index of the phase that produced the event in the {@link SolverConfig#getPhaseConfigList()}.
     * Is absent when the producer does not correspond to a phase, for instance,
     * an event triggered after {@link ProblemChange} were processed.
     *
     * @return The index of the corresponding phase in {@link SolverConfig#getPhaseConfigList()},
     *         or {@link OptionalInt#empty()} if there is no corresponding phase.
     */
    OptionalInt phaseIndex();

    static EventProducerId unknown() {
        return SolveEventProducerId.UNKNOWN;
    }

    static EventProducerId solvingStarted() {
        return SolveEventProducerId.SOLVING_STARTED;
    }

    static EventProducerId problemChange() {
        return SolveEventProducerId.PROBLEM_CHANGE;
    }

    /**
     * @deprecated Deprecated on account of {@link NoChangePhase} having no use.
     */
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
