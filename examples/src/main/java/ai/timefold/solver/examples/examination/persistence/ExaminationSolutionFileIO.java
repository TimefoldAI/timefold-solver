package ai.timefold.solver.examples.examination.persistence;

import ai.timefold.solver.examples.common.persistence.AbstractJsonSolutionFileIO;
import ai.timefold.solver.examples.examination.domain.Examination;

public class ExaminationSolutionFileIO extends AbstractJsonSolutionFileIO<Examination> {

    public ExaminationSolutionFileIO() {
        super(Examination.class);
    }
}
