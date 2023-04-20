package ai.timefold.solver.examples.examination.app;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.app.UnsolvedDirSolveAllTurtleTest;
import ai.timefold.solver.examples.examination.domain.Examination;

class ExaminationSolveAllTurtleTest extends UnsolvedDirSolveAllTurtleTest<Examination> {

    @Override
    protected CommonApp<Examination> createCommonApp() {
        return new ExaminationApp();
    }
}
