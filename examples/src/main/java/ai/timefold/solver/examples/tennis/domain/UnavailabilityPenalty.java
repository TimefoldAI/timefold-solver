package ai.timefold.solver.examples.tennis.domain;

import ai.timefold.solver.examples.common.domain.AbstractPersistable;

public class UnavailabilityPenalty extends AbstractPersistable {

    private Team team;
    private Day day;

    public UnavailabilityPenalty() {
    }

    public UnavailabilityPenalty(long id, Team team, Day day) {
        super(id);
        this.team = team;
        this.day = day;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public Day getDay() {
        return day;
    }

    public void setDay(Day day) {
        this.day = day;
    }

}
