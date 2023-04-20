package ai.timefold.solver.examples.examination.domain;

import ai.timefold.solver.examples.common.domain.AbstractPersistable;
import ai.timefold.solver.examples.common.persistence.jackson.JacksonUniqueIdGenerator;
import ai.timefold.solver.examples.common.swingui.components.Labeled;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;

@JsonIdentityInfo(generator = JacksonUniqueIdGenerator.class)
public class Room extends AbstractPersistable implements Labeled {

    private int capacity;
    private int penalty;

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getPenalty() {
        return penalty;
    }

    public void setPenalty(int penalty) {
        this.penalty = penalty;
    }

    @Override
    public String getLabel() {
        return Long.toString(id);
    }

    @Override
    public String toString() {
        return Long.toString(id);
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public Room withId(long id) {
        this.setId(id);
        return this;
    }

    public Room withCapacity(int capacity) {
        this.setCapacity(capacity);
        return this;
    }

    public Room withPenalty(int penalty) {
        this.setPenalty(penalty);
        return this;
    }

}
