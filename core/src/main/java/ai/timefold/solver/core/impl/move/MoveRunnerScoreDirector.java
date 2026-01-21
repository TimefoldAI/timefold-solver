package ai.timefold.solver.core.impl.move;

import java.util.Collections;
import java.util.Map;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.impl.neighborhood.MoveRepository;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirector;
import ai.timefold.solver.core.impl.score.director.InnerScore;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

final class MoveRunnerScoreDirector<Solution_, Score_ extends Score<Score_>>
        extends AbstractScoreDirector<Solution_, Score_, MoveRunnerScoreDirectorFactory<Solution_, Score_>> {

    private boolean firstTrigger = true;
    private final boolean isDerived;

    private MoveRunnerScoreDirector(Builder<Solution_, Score_> builder, boolean isDerived) {
        super(builder);
        this.isDerived = isDerived;
        setMoveRepository(builder.moveRepository);
    }

    @Override
    public void setWorkingSolutionWithoutUpdatingShadows(Solution_ workingSolution) {
        super.setWorkingSolutionWithoutUpdatingShadows(workingSolution, ignored -> {
        });
    }

    @Override
    public boolean isDerived() {
        return isDerived;
    }

    @Override
    public InnerScore<Score_> calculateScore() {
        if (!isDerived && firstTrigger) {
            firstTrigger = false;
        }
        return InnerScore.fullyAssigned(scoreDirectorFactory.getScoreDefinition().getZeroScore());
    }

    @Override
    public Map<String, ConstraintMatchTotal<Score_>> getConstraintMatchTotalMap() {
        return Collections.emptyMap();
    }

    @Override
    public Map<Object, Indictment<Score_>> getIndictmentMap() {
        return Collections.emptyMap();
    }

    @Override
    public boolean requiresFlushing() {
        return false;
    }

    @NullMarked
    public static final class Builder<Solution_, Score_ extends Score<Score_>>
            extends
            AbstractScoreDirectorBuilder<Solution_, Score_, MoveRunnerScoreDirectorFactory<Solution_, Score_>, Builder<Solution_, Score_>> {

        @Nullable
        private MoveRepository<Solution_> moveRepository;

        public Builder(MoveRunnerScoreDirectorFactory<Solution_, Score_> scoreDirectorFactory) {
            super(scoreDirectorFactory);
        }

        public Builder<Solution_, Score_> withMoveRepository(@Nullable MoveRepository<Solution_> moveRepository) {
            this.moveRepository = moveRepository;
            return this;
        }

        @Override
        public MoveRunnerScoreDirector<Solution_, Score_> build() {
            return new MoveRunnerScoreDirector<>(this, false);
        }

        @Override
        public MoveRunnerScoreDirector<Solution_, Score_> buildDerived() {
            return new MoveRunnerScoreDirector<>(this, true);
        }
    }

}