package ai.timefold.solver.core.testdomain.list.valuerange;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;

@PlanningSolution
public class TestdataListEntityProvidingSolution {

    public static SolutionDescriptor<TestdataListEntityProvidingSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataListEntityProvidingSolution.class,
                TestdataListEntityProvidingEntity.class, TestdataListEntityProvidingValue.class);
    }

    public static PlanningSolutionMetaModel<TestdataListEntityProvidingSolution> buildMetaModel() {
        return buildSolutionDescriptor().getMetaModel();
    }

    public static TestdataListEntityProvidingSolution generateSolution(int valueListSize, int entityListSize,
            boolean sameValueRange) {
        var solution = new TestdataListEntityProvidingSolution();
        var valueList = new ArrayList<TestdataListEntityProvidingValue>(valueListSize);
        for (var i = 0; i < valueListSize; i++) {
            var value = new TestdataListEntityProvidingValue("Generated Value " + i);
            valueList.add(value);
        }
        var entityList = new ArrayList<TestdataListEntityProvidingEntity>(entityListSize);
        var idx = 0;
        for (var i = 0; i < entityListSize; i++) {
            var valueRange = new ArrayList<TestdataListEntityProvidingValue>();
            if (sameValueRange) {
                valueRange.addAll(valueList);
            } else {
                var expectedCount = Math.max(1, valueListSize / entityListSize);
                for (var j = 0; j < expectedCount; j++) {
                    if (idx >= valueListSize) {
                        break;
                    }
                    valueRange.add(valueList.get(idx++));
                }
            }
            var entity = new TestdataListEntityProvidingEntity("Generated Entity " + i, valueRange);
            entityList.add(entity);
        }
        solution.setEntityList(entityList);
        return solution;
    }

    public static TestdataListEntityProvidingSolution generateSolution() {
        var solution = new TestdataListEntityProvidingSolution();
        var value1 = new TestdataListEntityProvidingValue("v1");
        var value2 = new TestdataListEntityProvidingValue("v2");
        var value3 = new TestdataListEntityProvidingValue("v3");
        var entity1 = new TestdataListEntityProvidingEntity("e1", List.of(value1, value2));
        var entity2 = new TestdataListEntityProvidingEntity("e2", List.of(value1, value3));
        solution.setEntityList(List.of(entity1, entity2));
        return solution;
    }

    private List<TestdataListEntityProvidingEntity> entityList;

    private SimpleScore score;

    @PlanningEntityCollectionProperty
    public List<TestdataListEntityProvidingEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataListEntityProvidingEntity> entityList) {
        this.entityList = entityList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataListEntityProvidingValue> getValueList() {
        return entityList.stream()
                .flatMap(entity -> entity.getValueRange().stream())
                .distinct()
                .toList();
    }

    @PlanningScore
    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

}
