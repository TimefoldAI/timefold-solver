package ai.timefold.solver.examples.nurserostering.persistence;

import java.io.File;
import java.util.function.Function;
import java.util.stream.Collectors;

import ai.timefold.solver.examples.common.persistence.AbstractJsonSolutionFileIO;
import ai.timefold.solver.examples.nurserostering.domain.Employee;
import ai.timefold.solver.examples.nurserostering.domain.NurseRoster;
import ai.timefold.solver.examples.nurserostering.domain.Shift;
import ai.timefold.solver.examples.nurserostering.domain.ShiftDate;

public class NurseRosterSolutionFileIO extends AbstractJsonSolutionFileIO<NurseRoster> {

    public NurseRosterSolutionFileIO() {
        super(NurseRoster.class);
    }

    @Override
    public NurseRoster read(File inputSolutionFile) {
        NurseRoster nurseRoster = super.read(inputSolutionFile);

        /*
         * Replace the duplicate Shift/ShiftDate instances by references to instances from the shiftList/shiftDateList.
         */
        var requestsById = nurseRoster.getShiftDateList().stream()
                .collect(Collectors.toMap(ShiftDate::getId, Function.identity()));
        var shiftsById = nurseRoster.getShiftList().stream()
                .collect(Collectors.toMap(Shift::getId, Function.identity()));
        for (Employee employee : nurseRoster.getEmployeeList()) {
            employee.setDayOffRequestMap(deduplicateMap(employee.getDayOffRequestMap(), requestsById, ShiftDate::getId));
            employee.setDayOnRequestMap(deduplicateMap(employee.getDayOnRequestMap(), requestsById, ShiftDate::getId));
            employee.setShiftOffRequestMap(deduplicateMap(employee.getShiftOffRequestMap(), shiftsById, Shift::getId));
            employee.setShiftOnRequestMap(deduplicateMap(employee.getShiftOnRequestMap(), shiftsById, Shift::getId));
        }

        return nurseRoster;
    }

}
