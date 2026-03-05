package ai.timefold.solver.quarkus.testdomain.spec;

import java.util.List;

import ai.timefold.solver.core.api.score.SimpleScore;

public class TestdataSpecSolution {

    private List<String> valueList;
    private List<TestdataSpecEntity> entityList;
    private SimpleScore score;

    public List<String> getValueList() {
        return valueList;
    }

    public void setValueList(List<String> valueList) {
        this.valueList = valueList;
    }

    public List<TestdataSpecEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataSpecEntity> entityList) {
        this.entityList = entityList;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

}
