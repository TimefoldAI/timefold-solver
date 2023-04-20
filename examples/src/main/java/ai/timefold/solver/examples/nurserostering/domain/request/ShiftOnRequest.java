package ai.timefold.solver.examples.nurserostering.domain.request;

import ai.timefold.solver.examples.common.domain.AbstractPersistable;
import ai.timefold.solver.examples.common.persistence.jackson.JacksonUniqueIdGenerator;
import ai.timefold.solver.examples.nurserostering.domain.Employee;
import ai.timefold.solver.examples.nurserostering.domain.Shift;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;

@JsonIdentityInfo(generator = JacksonUniqueIdGenerator.class)
public class ShiftOnRequest extends AbstractPersistable {

    private Employee employee;
    private Shift shift;
    private int weight;

    public ShiftOnRequest() {
    }

    public ShiftOnRequest(long id, Employee employee, Shift shift, int weight) {
        super(id);
        this.employee = employee;
        this.shift = shift;
        this.weight = weight;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public Shift getShift() {
        return shift;
    }

    public void setShift(Shift shift) {
        this.shift = shift;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    @Override
    public String toString() {
        return shift + "_ON_" + employee;
    }

}
