package ai.timefold.solver.core.impl.testdata;

import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.stream.common.InnerConstraintFactory;

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
    public String getDefaultConstraintPackage() {
        return "constraintPackage";
    }

    @Override
    public <A> UniConstraintStream<A> forEach(Class<A> sourceClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <A> UniConstraintStream<A> forEachIncludingUnassigned(Class<A> sourceClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <A> UniConstraintStream<A> from(Class<A> fromClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <A> UniConstraintStream<A> fromUnfiltered(Class<A> fromClass) {
        throw new UnsupportedOperationException();
    }
}
