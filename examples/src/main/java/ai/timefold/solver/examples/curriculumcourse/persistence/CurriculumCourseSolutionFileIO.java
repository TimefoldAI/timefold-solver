package ai.timefold.solver.examples.curriculumcourse.persistence;

import ai.timefold.solver.examples.common.persistence.AbstractJsonSolutionFileIO;
import ai.timefold.solver.examples.curriculumcourse.domain.CourseSchedule;

public class CurriculumCourseSolutionFileIO extends AbstractJsonSolutionFileIO<CourseSchedule> {

    public CurriculumCourseSolutionFileIO() {
        super(CourseSchedule.class);
    }
}
