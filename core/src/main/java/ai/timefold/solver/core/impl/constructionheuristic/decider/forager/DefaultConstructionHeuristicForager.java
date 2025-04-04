package ai.timefold.solver.core.impl.constructionheuristic.decider.forager;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.config.constructionheuristic.decider.forager.ConstructionHeuristicPickEarlyType;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicMoveScope;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicStepScope;
import ai.timefold.solver.core.impl.score.director.InnerScore;

public class DefaultConstructionHeuristicForager<Solution_> extends AbstractConstructionHeuristicForager<Solution_> {

    protected final ConstructionHeuristicPickEarlyType pickEarlyType;

    protected long selectedMoveCount;
    protected ConstructionHeuristicMoveScope<Solution_> earlyPickedMoveScope;
    protected ConstructionHeuristicMoveScope<Solution_> maxScoreMoveScope;

    public DefaultConstructionHeuristicForager(ConstructionHeuristicPickEarlyType pickEarlyType) {
        this.pickEarlyType = pickEarlyType;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public void stepStarted(ConstructionHeuristicStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        selectedMoveCount = 0L;
        earlyPickedMoveScope = null;
        maxScoreMoveScope = null;
    }

    @Override
    public void stepEnded(ConstructionHeuristicStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        earlyPickedMoveScope = null;
        maxScoreMoveScope = null;
    }

    @Override
    public void addMove(ConstructionHeuristicMoveScope<Solution_> moveScope) {
        selectedMoveCount++;
        moveScope.getStepScope().getPhaseScope()
                .addMoveEvaluationCount(moveScope.getMove(), 1L);
        checkPickEarly(moveScope);
        if (maxScoreMoveScope == null || moveScope.getScore().compareTo((InnerScore) maxScoreMoveScope.getScore()) > 0) {
            maxScoreMoveScope = moveScope;
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void checkPickEarly(ConstructionHeuristicMoveScope<Solution_> moveScope) {
        switch (pickEarlyType) {
            case NEVER -> {
            }
            case FIRST_NON_DETERIORATING_SCORE -> {
                Score lastStepScore = moveScope.getStepScope().getPhaseScope()
                        .getLastCompletedStepScope().getScore().initialized();
                Score moveScore = moveScope.getScore().initialized();
                if (moveScore.compareTo(lastStepScore) >= 0) {
                    earlyPickedMoveScope = moveScope;
                }
            }
            case FIRST_FEASIBLE_SCORE -> {
                if (moveScope.getScore().initialized().isFeasible()) {
                    earlyPickedMoveScope = moveScope;
                }
            }
            case FIRST_FEASIBLE_SCORE_OR_NON_DETERIORATING_HARD -> {
                Score lastStepScore = moveScope.getStepScope().getPhaseScope()
                        .getLastCompletedStepScope().getScore().initialized();
                Score moveScore = moveScope.getScore().initialized();
                Score lastStepScoreDifference = moveScore.subtract(lastStepScore);
                if (lastStepScoreDifference.isFeasible()) {
                    earlyPickedMoveScope = moveScope;
                }
            }
            default ->
                throw new IllegalStateException("The pickEarlyType (%s) is not implemented.".formatted(pickEarlyType));
        }
    }

    @Override
    public boolean isQuitEarly() {
        return earlyPickedMoveScope != null;
    }

    @Override
    public ConstructionHeuristicMoveScope<Solution_> pickMove(ConstructionHeuristicStepScope<Solution_> stepScope) {
        stepScope.setSelectedMoveCount(selectedMoveCount);
        if (earlyPickedMoveScope != null) {
            return earlyPickedMoveScope;
        } else {
            return maxScoreMoveScope;
        }
    }

}
