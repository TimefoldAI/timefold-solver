package ai.timefold.solver.examples.tennis.domain;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.examples.common.domain.AbstractPersistable;

@PlanningSolution
public class TennisSolution extends AbstractPersistable {

    private List<Team> teamList;
    private List<Day> dayList;
    private List<UnavailabilityPenalty> unavailabilityPenaltyList;

    private List<TeamAssignment> teamAssignmentList;

    private HardMediumSoftScore score;

    public TennisSolution() {
    }

    public TennisSolution(long id) {
        super(id);
    }

    @ValueRangeProvider
    @ProblemFactCollectionProperty
    public List<Team> getTeamList() {
        return teamList;
    }

    public void setTeamList(List<Team> teamList) {
        this.teamList = teamList;
    }

    @ProblemFactCollectionProperty
    public List<Day> getDayList() {
        return dayList;
    }

    public void setDayList(List<Day> dayList) {
        this.dayList = dayList;
    }

    @ProblemFactCollectionProperty
    public List<UnavailabilityPenalty> getUnavailabilityPenaltyList() {
        return unavailabilityPenaltyList;
    }

    public void setUnavailabilityPenaltyList(List<UnavailabilityPenalty> unavailabilityPenaltyList) {
        this.unavailabilityPenaltyList = unavailabilityPenaltyList;
    }

    @PlanningEntityCollectionProperty
    public List<TeamAssignment> getTeamAssignmentList() {
        return teamAssignmentList;
    }

    public void setTeamAssignmentList(List<TeamAssignment> teamAssignmentList) {
        this.teamAssignmentList = teamAssignmentList;
    }

    @PlanningScore
    public HardMediumSoftScore getScore() {
        return score;
    }

    public void setScore(HardMediumSoftScore score) {
        this.score = score;
    }

}
