package ai.timefold.solver.core.impl.testdata.domain.chained;

import java.util.List;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

@PlanningSolution
public class TestdataChainedSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataChainedSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataChainedSolution.class, TestdataChainedEntity.class);
    }

    private List<TestdataChainedAnchor> chainedAnchorList;
    private List<TestdataChainedEntity> chainedEntityList;
    private List<TestdataValue> unchainedValueList;

    private SimpleScore score;

    public TestdataChainedSolution() {
    }

    public TestdataChainedSolution(String code) {
        super(code);
    }

    @ValueRangeProvider(id = "chainedAnchorRange")
    @ProblemFactCollectionProperty
    public List<TestdataChainedAnchor> getChainedAnchorList() {
        return chainedAnchorList;
    }

    public void setChainedAnchorList(List<TestdataChainedAnchor> chainedAnchorList) {
        this.chainedAnchorList = chainedAnchorList;
    }

    @PlanningEntityCollectionProperty
    @ValueRangeProvider(id = "chainedEntityRange")
    public List<TestdataChainedEntity> getChainedEntityList() {
        return chainedEntityList;
    }

    public void setChainedEntityList(List<TestdataChainedEntity> chainedEntityList) {
        this.chainedEntityList = chainedEntityList;
    }

    @ValueRangeProvider(id = "unchainedRange")
    public List<TestdataValue> getUnchainedValueList() {
        return unchainedValueList;
    }

    public void setUnchainedValueList(List<TestdataValue> unchainedValueList) {
        this.unchainedValueList = unchainedValueList;
    }

    @PlanningScore
    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    public static TestdataChainedSolution generateUninitializedSolution(int valueCount, int entityCount) {
        return generateSolution(valueCount, entityCount);
    }

    private static TestdataChainedSolution generateSolution(int valueCount, int entityCount) {
        List<TestdataChainedEntity> entityList = IntStream.range(0, entityCount)
                .mapToObj(i -> new TestdataChainedEntity("Generated Entity " + i))
                .toList();
        List<TestdataChainedAnchor> anchorList = IntStream.range(0, entityCount)
                .mapToObj(i -> new TestdataChainedAnchor("Generated Anchor " + i))
                .toList();
        List<TestdataValue> valueList = IntStream.range(0, valueCount)
                .mapToObj(i -> new TestdataValue("Generated Value " + i))
                .toList();
        TestdataChainedSolution solution = new TestdataChainedSolution();
        solution.setChainedEntityList(entityList);
        solution.setChainedAnchorList(anchorList);
        solution.setUnchainedValueList(valueList);
        return solution;
    }

}
