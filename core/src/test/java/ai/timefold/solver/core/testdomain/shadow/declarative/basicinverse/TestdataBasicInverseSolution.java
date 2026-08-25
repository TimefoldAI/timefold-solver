package ai.timefold.solver.core.testdomain.shadow.declarative.basicinverse;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;

@PlanningSolution
public class TestdataBasicInverseSolution {

    @PlanningEntityCollectionProperty
    List<TestdataBasicInverseEntity> entityList;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider(id = "groupRange")
    List<TestdataBasicInverseGroup> groupList;

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "ownerRange")
    List<TestdataBasicInverseOwner> ownerList;

    @PlanningScore
    SimpleScore score;

    public TestdataBasicInverseSolution() {
    }

    public TestdataBasicInverseSolution(List<TestdataBasicInverseEntity> entityList,
            List<TestdataBasicInverseGroup> groupList, List<TestdataBasicInverseOwner> ownerList) {
        this.entityList = entityList;
        this.groupList = groupList;
        this.ownerList = ownerList;
    }

    public List<TestdataBasicInverseEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataBasicInverseEntity> entityList) {
        this.entityList = entityList;
    }

    public List<TestdataBasicInverseGroup> getGroupList() {
        return groupList;
    }

    public void setGroupList(List<TestdataBasicInverseGroup> groupList) {
        this.groupList = groupList;
    }

    public List<TestdataBasicInverseOwner> getOwnerList() {
        return ownerList;
    }

    public void setOwnerList(List<TestdataBasicInverseOwner> ownerList) {
        this.ownerList = ownerList;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
