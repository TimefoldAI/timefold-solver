package ai.timefold.solver.examples.travelingtournament.solver.move;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.examples.travelingtournament.domain.Day;
import ai.timefold.solver.examples.travelingtournament.domain.Match;
import ai.timefold.solver.examples.travelingtournament.domain.TravelingTournament;

public class TravelingTournamentMoveHelper {

    public static void moveDay(ScoreDirector<TravelingTournament> scoreDirector, Match match, Day toDay) {
        scoreDirector.beforeVariableChanged(match, "day");
        match.setDay(toDay);
        scoreDirector.afterVariableChanged(match, "day");
    }

    private TravelingTournamentMoveHelper() {
    }

}
