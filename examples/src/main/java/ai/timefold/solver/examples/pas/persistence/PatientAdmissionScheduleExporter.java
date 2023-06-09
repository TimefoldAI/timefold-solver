package ai.timefold.solver.examples.pas.persistence;

import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingLong;

import java.io.IOException;
import java.util.Comparator;

import ai.timefold.solver.examples.common.persistence.AbstractTxtSolutionExporter;
import ai.timefold.solver.examples.common.persistence.SolutionConverter;
import ai.timefold.solver.examples.pas.app.PatientAdmissionScheduleApp;
import ai.timefold.solver.examples.pas.domain.AdmissionPart;
import ai.timefold.solver.examples.pas.domain.Bed;
import ai.timefold.solver.examples.pas.domain.BedDesignation;
import ai.timefold.solver.examples.pas.domain.Patient;
import ai.timefold.solver.examples.pas.domain.PatientAdmissionSchedule;

public class PatientAdmissionScheduleExporter extends AbstractTxtSolutionExporter<PatientAdmissionSchedule> {

    public static void main(String[] args) {
        SolutionConverter<PatientAdmissionSchedule> converter = SolutionConverter.createExportConverter(
                PatientAdmissionScheduleApp.DATA_DIR_NAME, new PatientAdmissionScheduleExporter(),
                new PatientAdmissionScheduleSolutionFileIO());
        converter.convertAll();
    }

    @Override
    public TxtOutputBuilder<PatientAdmissionSchedule> createTxtOutputBuilder() {
        return new PatientAdmissionScheduleOutputBuilder();
    }

    public static class PatientAdmissionScheduleOutputBuilder extends TxtOutputBuilder<PatientAdmissionSchedule> {

        private static final Comparator<BedDesignation> COMPARATOR = comparing(BedDesignation::getAdmissionPart,
                comparingLong(AdmissionPart::getId))
                .thenComparing(BedDesignation::getBed, comparingLong(Bed::getId))
                .thenComparingLong(BedDesignation::getId);

        @Override
        public void writeSolution() throws IOException {
            solution.getBedDesignationList().sort(COMPARATOR);
            for (Patient patient : solution.getPatientList()) {
                bufferedWriter.write(Long.toString(patient.getId()));
                for (BedDesignation bedDesignation : solution.getBedDesignationList()) {
                    if (bedDesignation.getPatient().equals(patient)) {
                        for (int i = 0; i < bedDesignation.getAdmissionPart().getNightCount(); i++) {
                            bufferedWriter.write(" " + bedDesignation.getBed().getId());
                        }
                    }
                }
                bufferedWriter.write("\n");
            }
        }
    }

}
