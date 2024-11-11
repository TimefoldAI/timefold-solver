package ai.timefold.solver.test.impl.score.stream;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;

import org.jspecify.annotations.NonNull;

public final class DefaultConstraintVerifier<ConstraintProvider_ extends ConstraintProvider, Solution_, Score_ extends Score<Score_>>
        implements ConstraintVerifier<ConstraintProvider_, Solution_> {

    private final ConstraintProvider_ constraintProvider;
    private final SolutionDescriptor<Solution_> solutionDescriptor;
    /**
     * {@link ConstraintVerifier} is mutable,
     * due to {@link #withConstraintStreamImplType(ConstraintStreamImplType)}.
     * Since this method can be run at any time, possibly invalidating the pre-built score director factories,
     * the easiest way of dealing with the issue is to keep an internal immutable constraint verifier instance
     * and clearing it every time the configuration changes.
     * The code that was using the old configuration will continue running on the old instance,
     * which will eventually be garbage-collected.
     * Any new code will get a new instance with the new configuration applied.
     */
    private final AtomicReference<ConfiguredConstraintVerifier<ConstraintProvider_, Solution_, Score_>> configuredConstraintVerifierRef =
            new AtomicReference<>();

    public DefaultConstraintVerifier(ConstraintProvider_ constraintProvider, SolutionDescriptor<Solution_> solutionDescriptor) {
        this.constraintProvider = constraintProvider;
        this.solutionDescriptor = solutionDescriptor;
    }

    // ************************************************************************
    // Verify methods
    // ************************************************************************

    @Override
    public @NonNull DefaultSingleConstraintVerification<Solution_, Score_> verifyThat(
            @NonNull BiFunction<ConstraintProvider_, ConstraintFactory, Constraint> constraintFunction) {
        return getOrCreateConfiguredConstraintVerifier().verifyThat(constraintFunction);
    }

    private ConfiguredConstraintVerifier<ConstraintProvider_, Solution_, Score_> getOrCreateConfiguredConstraintVerifier() {
        return configuredConstraintVerifierRef.updateAndGet(v -> {
            if (v == null) {
                return new ConfiguredConstraintVerifier<>(constraintProvider, solutionDescriptor);
            }
            return v;
        });
    }

    @Override
    public @NonNull DefaultMultiConstraintVerification<Solution_, Score_> verifyThat() {
        return getOrCreateConfiguredConstraintVerifier().verifyThat();
    }

}
