package ai.timefold.solver.core.testdomain.declarative.follower;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningSolution
public class TestdataFollowerSolution extends TestdataObject {
    public static SolutionDescriptor<TestdataFollowerSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(Set.of(PreviewFeature.DECLARATIVE_SHADOW_VARIABLES),
                TestdataFollowerSolution.class, TestdataLeaderEntity.class, TestdataFollowerEntity.class);
    }

    @PlanningEntityCollectionProperty
    List<TestdataLeaderEntity> leaders;

    @PlanningEntityCollectionProperty
    List<TestdataFollowerEntity> followers;

    @ValueRangeProvider
    List<TestdataValue> values;

    @PlanningScore
    SimpleScore score;

    public TestdataFollowerSolution() {
    }

    public TestdataFollowerSolution(String code, List<TestdataLeaderEntity> leaders, List<TestdataFollowerEntity> followers,
            List<TestdataValue> values) {
        super(code);
        this.leaders = leaders;
        this.followers = followers;
        this.values = values;
    }

    public static TestdataFollowerSolution generateSolution(int leaderCount, int followerCount, int valueCount) {
        var random = new Random(0);
        var leaders = new ArrayList<TestdataLeaderEntity>(leaderCount);
        var followers = new ArrayList<TestdataFollowerEntity>(followerCount);
        var values = new ArrayList<TestdataValue>(valueCount);

        for (int i = 0; i < leaderCount; i++) {
            leaders.add(new TestdataLeaderEntity("Leader %d".formatted(i)));
        }

        for (var i = 0; i < followerCount; i++) {
            var leader = leaders.get(random.nextInt(leaders.size()));
            followers.add(new TestdataFollowerEntity("Follower %d leader %s".formatted(i, leader), leader));
        }

        for (var i = 0; i < valueCount; i++) {
            values.add(new TestdataValue("Value %d".formatted(i)));
        }

        return new TestdataFollowerSolution("Solution", leaders, followers, values);
    }

    public List<TestdataLeaderEntity> getLeaders() {
        return leaders;
    }

    public void setLeaders(List<TestdataLeaderEntity> leaders) {
        this.leaders = leaders;
    }

    public List<TestdataFollowerEntity> getFollowers() {
        return followers;
    }

    public void setFollowers(List<TestdataFollowerEntity> followers) {
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
