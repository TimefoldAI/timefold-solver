package ai.timefold.solver.core.impl.solver;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirector;
import ai.timefold.solver.core.impl.score.director.InnerScore;

import org.jspecify.annotations.NullMarked;

public final class MoveAssertScoreDirector<Solution_, Score_ extends Score<Score_>>
        extends AbstractScoreDirector<Solution_, Score_, MoveAssertScoreDirectorFactory<Solution_, Score_>> {

    private final Consumer<Solution_> moveSolutionConsumer;
    private boolean firstTrigger = true;
    private final boolean isDerived;

    private MoveAssertScoreDirector(Builder<Solution_, Score_> builder, boolean isDerived) {
        super(builder);
        this.moveSolutionConsumer = Objects.requireNonNull(builder.moveSolutionConsumer);
        this.isDerived = isDerived;
    }

    @Override
    public void setWorkingSolution(Solution_ workingSolution) {
        super.setWorkingSolution(workingSolution, ignored -> {
        });
    }

    @Override
    public boolean isDerived() {
        return isDerived;
    }

    @Override
    public InnerScore<Score_> calculateScore() {
        if (!isDerived && firstTrigger) {
            moveSolutionConsumer.accept(getWorkingSolution());
            firstTrigger = false;
        }
        return InnerScore.fullyAssigned(scoreDirectorFactory.getScoreDefinition().getZeroScore());
    }

    @Override
    public Map<String, ConstraintMatchTotal<Score_>> getConstraintMatchTotalMap() {
        return Map.of();
    }

    @Override
    public Map<Object, Indictment<Score_>> getIndictmentMap() {
        return Map.of();
    }

    @Override
    public boolean requiresFlushing() {
        return false;
    }

    @NullMarked
    public static final class Builder<Solution_, Score_ extends Score<Score_>>
            extends
            AbstractScoreDirectorBuilder<Solution_, Score_, MoveAssertScoreDirectorFactory<Solution_, Score_>, MoveAssertScoreDirector.Builder<Solution_, Score_>> {

        private Consumer<Solution_> moveSolutionConsumer;

        public Builder(MoveAssertScoreDirectorFactory<Solution_, Score_> scoreDirectorFactory) {
            super(scoreDirectorFactory);
        }

        public Builder<Solution_, Score_> withMoveSolutionConsumer(Consumer<Solution_> moveSolutionConsumer) {
            this.moveSolutionConsumer = moveSolutionConsumer;
            return this;
        }

        @Override
        public MoveAssertScoreDirector<Solution_, Score_> build() {
            return new MoveAssertScoreDirector<>(this, false);
        }

        @Override
        public MoveAssertScoreDirector<Solution_, Score_> buildDerived() {
            return new MoveAssertScoreDirector<>(this, true);
        }
    }

}