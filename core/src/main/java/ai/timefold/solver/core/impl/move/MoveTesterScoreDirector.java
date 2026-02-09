package ai.timefold.solver.core.impl.move;

import java.util.Collections;
import java.util.Map;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirector;
import ai.timefold.solver.core.impl.score.director.InnerScore;

import org.jspecify.annotations.NullMarked;

final class MoveTesterScoreDirector<Solution_, Score_ extends Score<Score_>>
        extends AbstractScoreDirector<Solution_, Score_, MoveTesterScoreDirectorFactory<Solution_, Score_>> {

    private MoveTesterScoreDirector(Builder<Solution_, Score_> builder) {
        super(builder);
    }

    @Override
    public void setWorkingSolutionWithoutUpdatingShadows(Solution_ workingSolution) {
        super.setWorkingSolutionWithoutUpdatingShadows(workingSolution, ignored -> {
        });
    }

    @Override
    public InnerScore<Score_> calculateScore() {
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
    static final class Builder<Solution_, Score_ extends Score<Score_>> extends
            AbstractScoreDirectorBuilder<Solution_, Score_, MoveTesterScoreDirectorFactory<Solution_, Score_>, Builder<Solution_, Score_>> {

        public Builder(MoveTesterScoreDirectorFactory<Solution_, Score_> scoreDirectorFactory) {
            super(scoreDirectorFactory);
        }

        @Override
        public MoveTesterScoreDirector<Solution_, Score_> build() {
            return new MoveTesterScoreDirector<>(this);
        }

        @Override
        public MoveTesterScoreDirector<Solution_, Score_> buildDerived() {
            throw new UnsupportedOperationException(); // This should never be derived, only used for testing.
        }
    }

}