package ai.timefold.solver.core.testdomain.shadow.dynamic_follower;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.shadow.follower.TestdataLeaderEntity;

@PlanningSolution
public class TestdataDynamicFollowerSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataDynamicFollowerSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataDynamicFollowerSolution.class, TestdataLeaderEntity.class, TestdataDynamicFollowerEntity.class);
    }

    public static PlanningSolutionMetaModel<TestdataDynamicFollowerSolution> buildMetaModel() {
        return buildSolutionDescriptor().getMetaModel();
    }

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataLeaderEntity> leaders;

    @PlanningEntityCollectionProperty
    List<TestdataDynamicFollowerEntity> followers;

    @ValueRangeProvider
    List<TestdataValue> values;

    @PlanningScore
    SimpleScore score;

    public TestdataDynamicFollowerSolution() {
    }

    public TestdataDynamicFollowerSolution(String code, List<TestdataLeaderEntity> leaders,
            List<TestdataDynamicFollowerEntity> followers,
            List<TestdataValue> values) {
        super(code);
        this.leaders = leaders;
        this.followers = followers;
        this.values = values;
    }

    public static TestdataDynamicFollowerSolution generateSolution(int leaderCount, int followerCount, int valueCount) {
        var leaders = new ArrayList<TestdataLeaderEntity>(leaderCount);
        var followers = new ArrayList<TestdataDynamicFollowerEntity>(followerCount);
        var values = new ArrayList<TestdataValue>(valueCount);

        for (int i = 0; i < leaderCount; i++) {
            leaders.add(new TestdataLeaderEntity("Leader %d".formatted(i)));
        }

        for (var i = 0; i < followerCount; i++) {
            followers.add(new TestdataDynamicFollowerEntity("Follower %d".formatted(i)));
        }

        for (var i = 0; i < valueCount; i++) {
            values.add(new TestdataValue("Value %d".formatted(i)));
        }

        return new TestdataDynamicFollowerSolution("Solution", leaders, followers, values);
    }

    public List<TestdataLeaderEntity> getLeaders() {
        return leaders;
    }

    public void setLeaders(List<TestdataLeaderEntity> leaders) {
        this.leaders = leaders;
    }

    public List<TestdataDynamicFollowerEntity> getFollowers() {
        return followers;
    }

    public void setFollowers(List<TestdataDynamicFollowerEntity> followers) {
        this.followers = followers;
    }

    public List<TestdataValue> getValues() {
        return values;
    }

    public void setValues(List<TestdataValue> values) {
        this.values = values;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
