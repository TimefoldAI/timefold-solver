package ai.timefold.solver.constraint.streams.common.inliner;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import ai.timefold.solver.constraint.streams.common.AbstractConstraint;
import ai.timefold.solver.constraint.streams.common.InnerConstraintFactory;
import ai.timefold.solver.constraint.streams.common.ScoreImpactType;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

public abstract class AbstractScoreInlinerTest<Solution_, Score_ extends Score<Score_>> {

    protected final boolean constraintMatchEnabled = true;
    private final TestConstraintFactory<Solution_, Score_> constraintFactory =
            new TestConstraintFactory<>(buildSolutionDescriptor());

    abstract protected SolutionDescriptor<Solution_> buildSolutionDescriptor();

    protected TestConstraint<Solution_, Score_> buildConstraint(Score_ constraintWeight) {
        return new TestConstraint<>(constraintFactory, "Test Constraint", constraintWeight);
    }

    protected WeightedScoreImpacter<Score_, ?> buildScoreImpacter(Score_ constraintWeight) {
        AbstractConstraint<?, ?, ?> constraint = buildConstraint(constraintWeight);
        Map<Constraint, Score_> constraintWeightMap = Collections.singletonMap(constraint, constraintWeight);
        AbstractScoreInliner<Score_> scoreInliner = buildScoreInliner(constraintWeightMap, constraintMatchEnabled);
        return scoreInliner.buildWeightedScoreImpacter(constraint);
    }

    abstract protected AbstractScoreInliner<Score_> buildScoreInliner(Map<Constraint, Score_> constraintWeightMap,
            boolean constraintMatchEnabled);

    public static final class TestConstraintFactory<Solution_, Score_ extends Score<Score_>>
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
        public <A> UniConstraintStream<A> forEachIncludingNullVars(Class<A> sourceClass) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <A> UniConstraintStream<A> fromUnfiltered(Class<A> fromClass) {
            throw new UnsupportedOperationException();
        }
    };

    public static final class TestConstraint<Solution_, Score_ extends Score<Score_>>
            extends AbstractConstraint<Solution_, TestConstraint<Solution_, Score_>, TestConstraintFactory<Solution_, Score_>> {

        protected TestConstraint(TestConstraintFactory<Solution_, Score_> constraintFactory, String constraintName,
                Score_ constraintWeight) {
            super(constraintFactory, constraintFactory.getDefaultConstraintPackage(), constraintName,
                    solution -> constraintWeight, ScoreImpactType.REWARD, false, null, null);
        }
    }

}
