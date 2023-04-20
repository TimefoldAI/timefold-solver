package ai.timefold.solver.spring.boot.autoconfigure.chained.domain;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;

@PlanningSolution
public class TestdataChainedSpringSolution {

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "chainedAnchorRange")
    private List<TestdataChainedSpringAnchor> chainedAnchorList;
    @PlanningEntityCollectionProperty
    @ValueRangeProvider(id = "chainedEntityRange")
    private List<TestdataChainedSpringEntity> chainedEntityList;

    @PlanningScore
    private SimpleScore score;

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    public List<TestdataChainedSpringAnchor> getChainedAnchorList() {
        return chainedAnchorList;
    }

    public void setChainedAnchorList(List<TestdataChainedSpringAnchor> chainedAnchorList) {
        this.chainedAnchorList = chainedAnchorList;
    }

    public List<TestdataChainedSpringEntity> getChainedEntityList() {
        return chainedEntityList;
    }

    public void setChainedEntityList(List<TestdataChainedSpringEntity> chainedEntityList) {
        this.chainedEntityList = chainedEntityList;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

}
