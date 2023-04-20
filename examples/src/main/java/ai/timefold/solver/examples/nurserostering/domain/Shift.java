package ai.timefold.solver.examples.nurserostering.domain;

import ai.timefold.solver.examples.common.domain.AbstractPersistable;
import ai.timefold.solver.examples.common.persistence.jackson.JacksonUniqueIdGenerator;
import ai.timefold.solver.examples.common.swingui.components.Labeled;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;

@JsonIdentityInfo(generator = JacksonUniqueIdGenerator.class)
public class Shift extends AbstractPersistable implements Labeled {

    private ShiftDate shiftDate;
    private ShiftType shiftType;
    private int index;

    private int requiredEmployeeSize;

    public Shift() {
    }

    public Shift(long id) {
        super(id);
    }

    public Shift(long id, ShiftDate shiftDate, ShiftType shiftType, int index, int requiredEmployeeSize) {
        this(id);
        this.shiftDate = shiftDate;
        this.shiftType = shiftType;
        this.index = index;
        this.requiredEmployeeSize = requiredEmployeeSize;
    }

    public ShiftDate getShiftDate() {
        return shiftDate;
    }

    public void setShiftDate(ShiftDate shiftDate) {
        this.shiftDate = shiftDate;
    }

    public ShiftType getShiftType() {
        return shiftType;
    }

    public void setShiftType(ShiftType shiftType) {
        this.shiftType = shiftType;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getRequiredEmployeeSize() {
        return requiredEmployeeSize;
    }

    public void setRequiredEmployeeSize(int requiredEmployeeSize) {
        this.requiredEmployeeSize = requiredEmployeeSize;
    }

    @Override
    public String getLabel() {
        return shiftType.getLabel() + " of " + shiftDate.getLabel();
    }

    @Override
    public String toString() {
        return shiftDate + "/" + shiftType;
    }

}
