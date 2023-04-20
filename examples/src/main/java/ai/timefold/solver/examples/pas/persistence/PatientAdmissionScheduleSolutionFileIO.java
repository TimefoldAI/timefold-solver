package ai.timefold.solver.examples.pas.persistence;

import ai.timefold.solver.examples.common.persistence.AbstractJsonSolutionFileIO;
import ai.timefold.solver.examples.pas.domain.PatientAdmissionSchedule;

public class PatientAdmissionScheduleSolutionFileIO extends AbstractJsonSolutionFileIO<PatientAdmissionSchedule> {

    public PatientAdmissionScheduleSolutionFileIO() {
        super(PatientAdmissionSchedule.class);
    }
}
