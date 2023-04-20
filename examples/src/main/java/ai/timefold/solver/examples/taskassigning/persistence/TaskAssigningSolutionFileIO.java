package ai.timefold.solver.examples.taskassigning.persistence;

import java.io.File;
import java.util.function.Function;
import java.util.stream.Collectors;

import ai.timefold.solver.examples.common.persistence.AbstractJsonSolutionFileIO;
import ai.timefold.solver.examples.taskassigning.domain.Customer;
import ai.timefold.solver.examples.taskassigning.domain.Employee;
import ai.timefold.solver.examples.taskassigning.domain.TaskAssigningSolution;

public class TaskAssigningSolutionFileIO extends AbstractJsonSolutionFileIO<TaskAssigningSolution> {

    public TaskAssigningSolutionFileIO() {
        super(TaskAssigningSolution.class);
    }

    @Override
    public TaskAssigningSolution read(File inputSolutionFile) {
        TaskAssigningSolution taskAssigningSolution = super.read(inputSolutionFile);

        var customersById = taskAssigningSolution.getCustomerList().stream()
                .collect(Collectors.toMap(Customer::getId, Function.identity()));
        /*
         * Replace the duplicate customer instances in the affinityMap by references to instances from
         * the customerList.
         */
        for (Employee employee : taskAssigningSolution.getEmployeeList()) {
            var newTravelDistanceMap = deduplicateMap(employee.getAffinityMap(),
                    customersById, Customer::getId);
            employee.setAffinityMap(newTravelDistanceMap);
        }
        return taskAssigningSolution;
    }

}
