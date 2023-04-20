package ai.timefold.solver.xstream.impl.testdata.domain;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;
import ai.timefold.solver.xstream.api.score.buildin.simple.SimpleScoreXStreamConverter;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;

@PlanningSolution
@XStreamAlias("xStreamTestdataSolution")
public class XStreamTestdataSolution extends TestdataObject {

    public static SolutionDescriptor<XStreamTestdataSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(XStreamTestdataSolution.class, XStreamTestdataEntity.class);
    }

    private List<XStreamTestdataValue> valueList;
    private List<XStreamTestdataEntity> entityList;

    @XStreamConverter(SimpleScoreXStreamConverter.class)
    private SimpleScore score;

    public XStreamTestdataSolution() {
    }

    public XStreamTestdataSolution(String code) {
        super(code);
    }

    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    public List<XStreamTestdataValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<XStreamTestdataValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<XStreamTestdataEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<XStreamTestdataEntity> entityList) {
        this.entityList = entityList;
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

}
