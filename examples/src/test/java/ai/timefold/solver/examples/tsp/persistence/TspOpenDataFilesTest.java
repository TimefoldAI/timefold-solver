package ai.timefold.solver.examples.tsp.persistence;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.persistence.OpenDataFilesTest;
import ai.timefold.solver.examples.tsp.app.TspApp;
import ai.timefold.solver.examples.tsp.domain.TspSolution;

class TspOpenDataFilesTest extends OpenDataFilesTest<TspSolution> {

    @Override
    protected CommonApp<TspSolution> createCommonApp() {
        return new TspApp();
    }
}
