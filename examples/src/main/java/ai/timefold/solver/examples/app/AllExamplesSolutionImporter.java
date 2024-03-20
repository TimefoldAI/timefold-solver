package ai.timefold.solver.examples.app;

import ai.timefold.solver.examples.common.app.LoggingMain;
import ai.timefold.solver.examples.examination.persistence.ExaminationImporter;
import ai.timefold.solver.examples.flightcrewscheduling.persistence.FlightCrewSchedulingGenerator;
import ai.timefold.solver.examples.meetingscheduling.persistence.MeetingSchedulingGenerator;
import ai.timefold.solver.examples.nurserostering.persistence.NurseRosteringImporter;
import ai.timefold.solver.examples.pas.persistence.PatientAdmissionScheduleImporter;
import ai.timefold.solver.examples.projectjobscheduling.persistence.ProjectJobSchedulingImporter;
import ai.timefold.solver.examples.taskassigning.persistence.TaskAssigningGenerator;
import ai.timefold.solver.examples.tennis.persistence.TennisGenerator;
import ai.timefold.solver.examples.travelingtournament.persistence.TravelingTournamentImporter;

public class AllExamplesSolutionImporter extends LoggingMain {

    public static void main(String[] args) {
        new AllExamplesSolutionImporter().importAll();
    }

    public void importAll() {
        ExaminationImporter.main(new String[0]);
        FlightCrewSchedulingGenerator.main(new String[0]);
        MeetingSchedulingGenerator.main(new String[0]);
        NurseRosteringImporter.main(new String[0]);
        PatientAdmissionScheduleImporter.main(new String[0]);
        ProjectJobSchedulingImporter.main(new String[0]);
        TaskAssigningGenerator.main(new String[0]);
        TennisGenerator.main(new String[0]);
        TravelingTournamentImporter.main(new String[0]);
    }

}
