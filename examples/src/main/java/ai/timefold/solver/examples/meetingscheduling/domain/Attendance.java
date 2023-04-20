package ai.timefold.solver.examples.meetingscheduling.domain;

import ai.timefold.solver.examples.common.domain.AbstractPersistable;

public abstract class Attendance extends AbstractPersistable {

    private Person person;
    private Meeting meeting;

    protected Attendance() {
    }

    protected Attendance(long id, Meeting meeting) {
        super(id);
        this.meeting = meeting;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public Meeting getMeeting() {
        return meeting;
    }

    public void setMeeting(Meeting meeting) {
        this.meeting = meeting;
    }

    @Override
    public String toString() {
        return person + "-" + meeting;
    }

}
