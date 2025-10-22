package ai.timefold.solver.core.testdomain.difficultyweight;

import ai.timefold.solver.core.api.domain.common.ComparatorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorterWeightFactory;

public class TestdataDifficultyFactory implements
        ComparatorFactory<TestdataDifficultyWeightSolution, TestdataDifficultyWeightEntity, TestdataDifficultyFactory.TestdataDifficultyWeightComparable>,
        SelectionSorterWeightFactory<TestdataDifficultyWeightSolution, TestdataDifficultyWeightEntity> {

    @Override
    public TestdataDifficultyWeightComparable createSorter(TestdataDifficultyWeightSolution solution,
            TestdataDifficultyWeightEntity entity) {
        return new TestdataDifficultyWeightComparable();
    }

    @Override
    public Comparable createSorterWeight(TestdataDifficultyWeightSolution testdataDifficultyWeightSolution,
            TestdataDifficultyWeightEntity selection) {
        return createSorter(testdataDifficultyWeightSolution, selection);
    }

    public static class TestdataDifficultyWeightComparable implements Comparable<TestdataDifficultyWeightComparable> {

        @Override
        public int compareTo(TestdataDifficultyWeightComparable other) {
            return 0;
        }
    }
}
