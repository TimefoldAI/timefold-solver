package ai.timefold.solver.core.api.solver.event;

import java.util.OptionalInt;

import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.api.solver.phase.PhaseId;
import ai.timefold.solver.core.api.solver.phase.PhaseType;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.impl.phase.event.DefaultPhaseEventProducerId;
import ai.timefold.solver.core.impl.solver.event.SolveEventProducerId;

import org.jspecify.annotations.NullMarked;

/**
 * Identifies the producer of a {@link BestSolutionChangedEvent}.
 * Do not attempt to parse this class' {@link Object#toString() toString()},
 * it is not guaranteed to be stable.
 *
 * @see PhaseEventProducerId A subtype in case a phase produced the event,
 *      which gives access to a regular {@link PhaseId}.
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

    /**
     * @deprecated this type isn't meant to be constructed by a user.
     *             To tell whether an event comes from a phase, check whether its producer is a {@link PhaseEventProducerId}.
     */
    @Deprecated(since = "2.9.0", forRemoval = true)
    static EventProducerId solvingStarted() {
        return SolveEventProducerId.SOLVING_STARTED;
    }

    /**
     * @deprecated this type isn't meant to be constructed by a user.
     *             To tell whether an event comes from a phase, check whether its producer is a {@link PhaseEventProducerId}.
     */
    @Deprecated(since = "2.9.0", forRemoval = true)
    static EventProducerId problemChange() {
        return SolveEventProducerId.PROBLEM_CHANGE;
    }

    /**
     * @deprecated this type isn't meant to be constructed by a user.
     *             To find the phase of an event, use {@link PhaseEventProducerId#phaseId()}.
     */
    @Deprecated(since = "2.9.0", forRemoval = true)
    static EventProducerId constructionHeuristic(int phaseIndex) {
        return DefaultPhaseEventProducerId.of(PhaseType.CONSTRUCTION_HEURISTIC, phaseIndex);
    }

    /**
     * @deprecated this type isn't meant to be constructed by a user.
     *             To find the phase of an event, use {@link PhaseEventProducerId#phaseId()}.
     */
    @Deprecated(since = "2.9.0", forRemoval = true)
    static EventProducerId localSearch(int phaseIndex) {
        return DefaultPhaseEventProducerId.of(PhaseType.LOCAL_SEARCH, phaseIndex);
    }

    /**
     * @deprecated this type isn't meant to be constructed by a user.
     *             To find the phase of an event, use {@link PhaseEventProducerId#phaseId()}.
     */
    @Deprecated(since = "2.9.0", forRemoval = true)
    static EventProducerId exhaustiveSearch(int phaseIndex) {
        return DefaultPhaseEventProducerId.of(PhaseType.EXHAUSTIVE_SEARCH, phaseIndex);
    }

    /**
     * @deprecated this type isn't meant to be constructed by a user.
     *             To find the phase of an event, use {@link PhaseEventProducerId#phaseId()}.
     */
    @Deprecated(since = "2.9.0", forRemoval = true)
    static EventProducerId partitionedSearch(int phaseIndex) {
        return DefaultPhaseEventProducerId.of(PhaseType.PARTITIONED_SEARCH, phaseIndex);
    }

    /**
     * @deprecated this type isn't meant to be constructed by a user.
     *             To find the phase of an event, use {@link PhaseEventProducerId#phaseId()}.
     */
    @Deprecated(since = "2.9.0", forRemoval = true)
    static EventProducerId customPhase(int phaseIndex) {
        return DefaultPhaseEventProducerId.of(PhaseType.CUSTOM_PHASE, phaseIndex);
    }
}
