package ai.timefold.solver.examples.meetingscheduling.domain;

import ai.timefold.solver.examples.common.domain.AbstractPersistable;
import ai.timefold.solver.examples.common.swingui.components.Labeled;

public class Person extends AbstractPersistable implements Labeled {

    private String fullName;

    public Person() {
    }

    public Person(long id, String fullName) {
        super(id);
        this.fullName = fullName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    @Override
    public String getLabel() {
        return fullName;
    }

    @Override
    public String toString() {
        return fullName;
    }

}
