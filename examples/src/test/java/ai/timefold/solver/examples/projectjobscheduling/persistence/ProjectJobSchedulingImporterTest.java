package ai.timefold.solver.examples.projectjobscheduling.persistence;

import ai.timefold.solver.examples.common.persistence.AbstractSolutionImporter;
import ai.timefold.solver.examples.common.persistence.ImportDataFilesTest;
import ai.timefold.solver.examples.projectjobscheduling.app.ProjectJobSchedulingApp;
import ai.timefold.solver.examples.projectjobscheduling.domain.Schedule;

class ProjectJobSchedulingImporterTest extends ImportDataFilesTest<Schedule> {

    @Override
    protected AbstractSolutionImporter<Schedule> createSolutionImporter() {
        return new ProjectJobSchedulingImporter();
    }

    @Override
    protected String getDataDirName() {
        return ProjectJobSchedulingApp.DATA_DIR_NAME;
    }
}
