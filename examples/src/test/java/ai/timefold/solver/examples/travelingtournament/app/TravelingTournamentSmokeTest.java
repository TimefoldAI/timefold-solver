package ai.timefold.solver.examples.travelingtournament.app;

import java.util.stream.Stream;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
import ai.timefold.solver.examples.common.app.SolverSmokeTest;
import ai.timefold.solver.examples.travelingtournament.domain.TravelingTournament;

class TravelingTournamentSmokeTest extends SolverSmokeTest<TravelingTournament, HardSoftScore> {

    private static final String UNSOLVED_DATA_FILE = "data/travelingtournament/unsolved/1-nl10.json";

    @Override
    protected TravelingTournamentApp createCommonApp() {
        return new TravelingTournamentApp();
    }

    @Override
    protected Stream<TestData<HardSoftScore>> testData() {
        return Stream.of(
                TestData.of(ConstraintStreamImplType.BAVET, UNSOLVED_DATA_FILE,
                        HardSoftScore.ofSoft(-72772),
                        HardSoftScore.ofSoft(-72772)));
    }
}
