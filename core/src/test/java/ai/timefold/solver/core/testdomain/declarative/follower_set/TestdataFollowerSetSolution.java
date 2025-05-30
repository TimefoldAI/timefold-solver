package ai.timefold.solver.core.testdomain.declarative.follower_set;

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
import ai.timefold.solver.core.testdomain.declarative.follower.TestdataLeaderEntity;

@PlanningSolution
public class TestdataFollowerSetSolution extends TestdataObject {
    public static SolutionDescriptor<TestdataFollowerSetSolution> getSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(Set.of(PreviewFeature.DECLARATIVE_SHADOW_VARIABLES),
                TestdataFollowerSetSolution.class, TestdataLeaderEntity.class, TestdataFollowerSetEntity.class);
    }

    @PlanningEntityCollectionProperty
    List<TestdataLeaderEntity> leaders;

    @PlanningEntityCollectionProperty
    List<TestdataFollowerSetEntity> followers;

    @ValueRangeProvider
    List<TestdataValue> values;

    @PlanningScore
    SimpleScore score;

    public TestdataFollowerSetSolution() {
    }

    public TestdataFollowerSetSolution(String code, List<TestdataLeaderEntity> leaders,
            List<TestdataFollowerSetEntity> followers,
            List<TestdataValue> values) {
        super(code);
        this.leaders = leaders;
        this.followers = followers;
        this.values = values;
    }

    public static TestdataFollowerSetSolution generateSolution(int leaderCount, int followerCount, int valueCount) {
        var random = new Random(0);
        var leaders = new ArrayList<TestdataLeaderEntity>(leaderCount);
        var followers = new ArrayList<TestdataFollowerSetEntity>(followerCount);
        var values = new ArrayList<TestdataValue>(valueCount);

        for (int i = 0; i < leaderCount; i++) {
            leaders.add(new TestdataLeaderEntity("Leader %d".formatted(i)));
        }

        for (var i = 0; i < followerCount; i++) {
            var leader1 = leaders.get(random.nextInt(leaders.size()));
            var leader2 = leaders.get(random.nextInt(leaders.size()));
            while (leader1 == leader2) {
                leader2 = leaders.get(random.nextInt(leaders.size()));
            }
            var followerLeaders = List.of(leader1, leader2);
            followers.add(
                    new TestdataFollowerSetEntity("Follower %d leaders %s".formatted(i, followerLeaders), followerLeaders));
        }

        for (var i = 0; i < valueCount; i++) {
            values.add(new TestdataValue("Value %d".formatted(i)));
        }

        return new TestdataFollowerSetSolution("Solution", leaders, followers, values);
    }

    public List<TestdataLeaderEntity> getLeaders() {
        return leaders;
    }

    public void setLeaders(List<TestdataLeaderEntity> leaders) {
        this.leaders = leaders;
    }

    public List<TestdataFollowerSetEntity> getFollowers() {
        return followers;
    }

    public void setFollowers(List<TestdataFollowerSetEntity> followers) {
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
