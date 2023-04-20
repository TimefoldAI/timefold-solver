package ai.timefold.solver.examples.pas.solver.move;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.examples.pas.domain.Bed;
import ai.timefold.solver.examples.pas.domain.BedDesignation;
import ai.timefold.solver.examples.pas.domain.PatientAdmissionSchedule;

public class PatientAdmissionMoveHelper {

    public static void moveBed(ScoreDirector<PatientAdmissionSchedule> scoreDirector, BedDesignation bedDesignation,
            Bed toBed) {
        scoreDirector.beforeVariableChanged(bedDesignation, "bed");
        bedDesignation.setBed(toBed);
        scoreDirector.afterVariableChanged(bedDesignation, "bed");
    }

    private PatientAdmissionMoveHelper() {
    }

}
