package ai.timefold.solver.examples.travelingtournament.solver.move.factory;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.SwapMove;
import ai.timefold.solver.examples.travelingtournament.domain.Match;
import ai.timefold.solver.examples.travelingtournament.domain.TravelingTournament;

public class InverseMatchSwapMoveFilter implements SelectionFilter<TravelingTournament, SwapMove> {

    @Override
    public boolean accept(ScoreDirector<TravelingTournament> scoreDirector, SwapMove move) {
        Match leftMatch = (Match) move.getLeftEntity();
        Match rightMatch = (Match) move.getRightEntity();
        return leftMatch.getHomeTeam().equals(rightMatch.getAwayTeam())
                && leftMatch.getAwayTeam().equals(rightMatch.getHomeTeam());
    }

}
