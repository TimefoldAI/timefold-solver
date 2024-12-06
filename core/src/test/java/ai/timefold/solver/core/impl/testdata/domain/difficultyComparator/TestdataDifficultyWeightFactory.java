package ai.timefold.solver.core.impl.testdata.domain.difficultyComparator;

import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorterWeightFactory;

public class TestdataDifficultyWeightFactory implements
        SelectionSorterWeightFactory<TestdataDifficultyWeightSolution, TestdataDifficultyWeightEntity> {

    @Override
    public TestdataDifficultyWeightComparable createSorterWeight(TestdataDifficultyWeightSolution solution,
            TestdataDifficultyWeightEntity entity) {
        return new TestdataDifficultyWeightComparable(entity);
    }

    public static class TestdataDifficultyWeightComparable implements Comparable<TestdataDifficultyWeightComparable> {

        private final TestdataDifficultyWeightEntity entity;

        public TestdataDifficultyWeightComparable(TestdataDifficultyWeightEntity entity) {
            this.entity = entity;
        }

        @Override
        public int compareTo(TestdataDifficultyWeightComparable other) {
            entity.setComparisonCalled(true);
            other.entity.setComparisonCalled(true);
            return 0;
        }
    }
}
