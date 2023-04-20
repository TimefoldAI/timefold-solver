package ai.timefold.solver.jaxb.impl.testdata.domain;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.jaxb.api.score.buildin.simple.SimpleScoreJaxbAdapter;

@PlanningSolution
@XmlRootElement

public class JaxbTestdataSolution extends JaxbTestdataObject {

    public static SolutionDescriptor<JaxbTestdataSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(JaxbTestdataSolution.class, JaxbTestdataEntity.class);
    }

    private List<JaxbTestdataValue> valueList;
    private List<JaxbTestdataEntity> entityList;

    private SimpleScore score;

    public JaxbTestdataSolution() {
    }

    public JaxbTestdataSolution(String code) {
        super(code);
    }

    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    @XmlElementWrapper(name = "valueList")
    @XmlElement(name = "jaxbTestdataValue")
    public List<JaxbTestdataValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<JaxbTestdataValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    @XmlElementWrapper(name = "entityList")
    @XmlElement(name = "jaxbTestdataEntity")
    public List<JaxbTestdataEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<JaxbTestdataEntity> entityList) {
        this.entityList = entityList;
    }

    @PlanningScore
    @XmlJavaTypeAdapter(SimpleScoreJaxbAdapter.class)
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
