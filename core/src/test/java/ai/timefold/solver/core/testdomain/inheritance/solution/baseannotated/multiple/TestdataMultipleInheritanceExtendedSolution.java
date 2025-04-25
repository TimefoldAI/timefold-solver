package ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.multiple;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;

@PlanningSolution
public class TestdataMultipleInheritanceExtendedSolution extends TestdataMultipleInheritanceChildSolution {

    public static TestdataMultipleInheritanceExtendedSolution generateSolution(int valueListSize, int entityListSize) {
        var solution = new TestdataMultipleInheritanceExtendedSolution();
        var valueList = new ArrayList<String>(valueListSize);
        for (int i = 0; i < valueListSize; i++) {
            valueList.add("Generated Value " + i);
        }
        solution.setValueList(valueList);
        var entityList = new ArrayList<TestdataMultipleInheritanceEntity>(entityListSize);
        var idx = 0;
        for (int i = 0; i < entityListSize; i++) {
            var entity = new TestdataMultipleInheritanceEntity(idx++);
            entityList.add(entity);
        }
        solution.setEntityList(entityList);
        return solution;
    }

    @Override
    public List<TestdataMultipleInheritanceEntity> getEntityList() {
        return (List<TestdataMultipleInheritanceEntity>) super.getEntityList();
    }
}
