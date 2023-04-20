package ai.timefold.solver.examples.projectjobscheduling.persistence;

import ai.timefold.solver.examples.common.persistence.AbstractJsonSolutionFileIO;
import ai.timefold.solver.examples.projectjobscheduling.domain.Schedule;

public class ProjectJobSchedulingSolutionFileIO extends AbstractJsonSolutionFileIO<Schedule> {

    public ProjectJobSchedulingSolutionFileIO() {
        super(Schedule.class);
    }
}
