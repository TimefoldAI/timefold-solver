package ai.timefold.solver.core.testdomain.difficultyweight;

import ai.timefold.solver.core.api.domain.common.SorterFactory;

public class TestdataDifficultyFactory implements
        SorterFactory<TestdataDifficultyWeightSolution, TestdataDifficultyWeightEntity> {

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
