package ai.timefold.solver.examples.tennis.persistence;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.persistence.OpenDataFilesTest;
import ai.timefold.solver.examples.tennis.app.TennisApp;
import ai.timefold.solver.examples.tennis.domain.TennisSolution;

class TennisOpenDataFilesTest extends OpenDataFilesTest<TennisSolution> {

    @Override
    protected CommonApp<TennisSolution> createCommonApp() {
        return new TennisApp();
    }
}
