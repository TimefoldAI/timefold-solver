package ai.timefold.solver.core.impl.solver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;

import org.jspecify.annotations.NullMarked;

/**
 * The single answer to "which {@link EnvironmentMode} does this {@link SolverConfig} actually run in".
 * <p>
 * {@link SolverConfig#determineEnvironmentMode()} only reports what the solver config declares.
 * Since a phase may override the environment mode, that declared value is no longer the whole truth:
 * a phase may run in a stricter mode than the solver,
 * and a config whose every phase agrees on one mode adopts that mode as its global one.
 * Both the ({@link DefaultSolverFactory}) and anything reporting on a config
 * (the benchmark report, notably) need those rules, so they live here rather than in either.
 * <p>
 * The resolution is split in two on purpose:
 * <ul>
 * <li>{@link #validate(SolverConfig)} throws on a config whose phase overrides break the rules;
 * it is the solver's job to reject such a config.</li>
 * <li>{@link #resolve(SolverConfig)} and the methods built on it are total and never throw,
 * because a reporting path runs long after the config was accepted
 * and must not be able to fail on validation.</li>
 * </ul>
 * A resolution of a config which never passed {@link #validate(SolverConfig)} is therefore best-effort:
 * it applies the rules to a config the solver would have refused to build.
 * <p>
 * Note that {@link EnvironmentMode} is declared from strictest to most lenient,
 * so a lower {@link Enum#ordinal()} means a stricter mode.
 */
@NullMarked
public final class EnvironmentModeResolver {

    /**
     * Fails on a config whose phase-level environment modes break either of the two rules:
     * no phase may be less strict than the global mode,
     * and a non-reproducible global mode admits no phase-level override at all.
     *
     * @throws IllegalStateException when a phase-level override is not allowed
     */
    public static void validate(SolverConfig solverConfig) {
        var globalEnvironmentMode = solverConfig.determineEnvironmentMode();
        var phaseEnvironmentModeList = determinePhaseEnvironmentModeList(solverConfig, globalEnvironmentMode);
        if (phaseEnvironmentModeList.isEmpty()) {
            return;
        }
        if (globalEnvironmentMode == EnvironmentMode.NON_REPRODUCIBLE
                && phaseEnvironmentModeList.stream().anyMatch(environmentMode -> environmentMode != globalEnvironmentMode)) {
            // A non-reproducible global environment mode cannot be overridden per phase,
            // as a phase-level override would have nothing reproducible to be an override of.
            throw new IllegalStateException(
                    "Phase-level environmentMode override is only possible when global environmentMode is reproducible, but was %s."
                            .formatted(globalEnvironmentMode.name()));
        }
        // Every phase may override the global mode, including all of them at once:
        // the global mode still applies outside the phases, and the factory built for it is needed regardless
        // by the components which are decoupled from the solving life cycle.
        var invalidPhaseEnvironmentList = new ArrayList<String>(phaseEnvironmentModeList.size());
        for (var phaseEnvironmentMode : phaseEnvironmentModeList) {
            if (phaseEnvironmentMode.ordinal() > globalEnvironmentMode.ordinal()) {
                invalidPhaseEnvironmentList.add(phaseEnvironmentMode.name());
            }
        }
        if (!invalidPhaseEnvironmentList.isEmpty()) {
            // The phase environments must have an assertion level greater than or equal to the global environment level
            throw new IllegalStateException(
                    "The phase environments must have an assertion level higher than or equal to the global environment level (%s). The following phase environment modes are not valid: [%s]."
                            .formatted(globalEnvironmentMode.name(), String.join(", ", invalidPhaseEnvironmentList)));
        }
    }

    /**
     * The environment mode the solver as a whole runs in,
     * which is the declared {@link SolverConfig#determineEnvironmentMode()}
     * unless every phase agrees on one mode, in which case that mode is adopted as the global one.
     * There is then nothing to swap away from mid-solve, which spares the solver a second score director
     * factory — and, with Constraint Streams, a second constraint network — for a mode no phase ever runs in.
     * <p>
     * This is not the strictest mode the solve runs in; see {@link #resolveStrictest(SolverConfig)} for that.
     *
     * @see #validate(SolverConfig) never throws, unlike the validation
     */
    public static EnvironmentMode resolve(SolverConfig solverConfig) {
        var globalEnvironmentMode = solverConfig.determineEnvironmentMode();
        var phaseEnvironmentModeList = determinePhaseEnvironmentModeList(solverConfig, globalEnvironmentMode);
        if (phaseEnvironmentModeList.isEmpty()) {
            return globalEnvironmentMode;
        }
        var distinctPhaseEnvironmentModeList = phaseEnvironmentModeList.stream().distinct().toList();
        return distinctPhaseEnvironmentModeList.size() == 1
                ? distinctPhaseEnvironmentModeList.getFirst()
                : globalEnvironmentMode;
    }

    /**
     * The environment mode of each phase, in the order of {@link SolverConfig#getPhaseConfigList()};
     * a phase which does not override the mode contributes {@link #resolve(SolverConfig)}.
     *
     * @return empty when the config declares no phases
     */
    public static List<EnvironmentMode> resolvePhases(SolverConfig solverConfig) {
        return determinePhaseEnvironmentModeList(solverConfig, resolve(solverConfig));
    }

    /**
     * The strictest environment mode any part of the solve runs in,
     * which is the strictest of {@link #resolve(SolverConfig)} and every phase's mode.
     * This is what a report has to look at to describe the cost of a config:
     * a single phase in {@link EnvironmentMode#FULL_ASSERT} slows the whole run down,
     * no matter how lenient the solver-level mode is.
     */
    public static EnvironmentMode resolveStrictest(SolverConfig solverConfig) {
        var strictestEnvironmentMode = resolve(solverConfig);
        for (var phaseEnvironmentMode : determinePhaseEnvironmentModeList(solverConfig, strictestEnvironmentMode)) {
            if (phaseEnvironmentMode.ordinal() < strictestEnvironmentMode.ordinal()) {
                strictestEnvironmentMode = phaseEnvironmentMode;
            }
        }
        return strictestEnvironmentMode;
    }

    private static List<EnvironmentMode> determinePhaseEnvironmentModeList(SolverConfig solverConfig,
            EnvironmentMode globalEnvironmentMode) {
        var phaseConfigList = solverConfig.getPhaseConfigList();
        if (phaseConfigList == null || phaseConfigList.isEmpty()) {
            return List.of();
        }
        return phaseConfigList.stream()
                .map(phaseConfig -> Objects.requireNonNullElse(phaseConfig.getEnvironmentMode(), globalEnvironmentMode))
                .toList();
    }

    private EnvironmentModeResolver() {
        // No external instances.
    }

}
