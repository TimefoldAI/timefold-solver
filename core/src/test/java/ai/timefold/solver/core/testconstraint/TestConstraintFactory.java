package ai.timefold.solver.core.testconstraint;

import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.stream.common.InnerConstraintFactory;

import org.jspecify.annotations.NonNull;

public final class TestConstraintFactory<Solution_, Score_ extends Score<Score_>>
        extends InnerConstraintFactory<Solution_, TestConstraint<Solution_, Score_>> {

    private final SolutionDescriptor<Solution_> solutionDescriptor;

    public TestConstraintFactory(SolutionDescriptor<Solution_> solutionDescriptor) {
        this.solutionDescriptor = Objects.requireNonNull(solutionDescriptor);
    }

    @Override
    public SolutionDescriptor<Solution_> getSolutionDescriptor() {
        return solutionDescriptor;
    }

    @Override
    public @NonNull String getDefaultConstraintPackage() {
        return "constraintPackage";
    }

    @Override
    public <A> @NonNull UniConstraintStream<A> forEach(@NonNull Class<A> sourceClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <A> @NonNull UniConstraintStream<A> forEachIncludingUnassigned(@NonNull Class<A> sourceClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <A> @NonNull UniConstraintStream<A> from(@NonNull Class<A> fromClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <A> @NonNull UniConstraintStream<A> fromUnfiltered(@NonNull Class<A> fromClass) {
        throw new UnsupportedOperationException();
    }
}
