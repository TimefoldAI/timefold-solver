package ai.timefold.solver.core.testdomain.difficultyweight;

import ai.timefold.solver.core.api.domain.common.ComparatorFactory;

public class TestdataDifficultyFactory implements
        ComparatorFactory<TestdataDifficultyWeightSolution, TestdataDifficultyWeightEntity> {

    @Override
    public TestdataDifficultyWeightComparable createSorter(TestdataDifficultyWeightSolution solution,
            TestdataDifficultyWeightEntity entity) {
        return new TestdataDifficultyWeightComparable();
    }

    public static class TestdataDifficultyWeightComparable implements Comparable<TestdataDifficultyWeightComparable> {

        @Override
        public int compareTo(TestdataDifficultyWeightComparable other) {
            return 0;
        }
    }
}
