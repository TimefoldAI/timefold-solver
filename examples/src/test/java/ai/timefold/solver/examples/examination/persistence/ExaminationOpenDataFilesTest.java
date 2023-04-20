package ai.timefold.solver.examples.examination.persistence;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.persistence.OpenDataFilesTest;
import ai.timefold.solver.examples.examination.app.ExaminationApp;
import ai.timefold.solver.examples.examination.domain.Examination;

class ExaminationOpenDataFilesTest extends OpenDataFilesTest<Examination> {

    @Override
    protected CommonApp<Examination> createCommonApp() {
        return new ExaminationApp();
    }
}
