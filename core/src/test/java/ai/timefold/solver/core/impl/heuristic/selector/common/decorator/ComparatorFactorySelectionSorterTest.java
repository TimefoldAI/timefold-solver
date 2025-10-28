package ai.timefold.solver.core.impl.heuristic.selector.common.decorator;

import static ai.timefold.solver.core.testutil.PlannerAssert.assertCodesOfIterator;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import ai.timefold.solver.core.api.domain.common.ComparatorFactory;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.config.heuristic.selector.common.decorator.SelectionSorterOrder;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.junit.jupiter.api.Test;

class ComparatorFactorySelectionSorterTest {

    @Test
    void sortAscending() {
        ComparatorFactory<TestdataSolution, TestdataEntity> comparatorFactory =
                sol -> Comparator.comparingInt(v -> Integer.valueOf(v.getCode().charAt(0)));
        ComparatorFactorySelectionSorter<TestdataSolution, TestdataEntity> selectionSorter =
                new ComparatorFactorySelectionSorter<>(
                        comparatorFactory, SelectionSorterOrder.ASCENDING);
        ScoreDirector<TestdataSolution> scoreDirector = mock(ScoreDirector.class);
        List<TestdataEntity> selectionList = new ArrayList<>();
        selectionList.add(new TestdataEntity("C"));
        selectionList.add(new TestdataEntity("A"));
        selectionList.add(new TestdataEntity("D"));
        selectionList.add(new TestdataEntity("B"));
        selectionSorter.sort(scoreDirector, selectionList);
        assertCodesOfIterator(selectionList.iterator(), "A", "B", "C", "D");
    }

    @Test
    void sortDescending() {
        ComparatorFactory<TestdataSolution, TestdataEntity> comparatorFactory =
                sol -> Comparator.comparingInt(v -> Integer.valueOf(v.getCode().charAt(0)));
        ComparatorFactorySelectionSorter<TestdataSolution, TestdataEntity> selectionSorter =
                new ComparatorFactorySelectionSorter<>(
                        comparatorFactory, SelectionSorterOrder.DESCENDING);
        ScoreDirector<TestdataSolution> scoreDirector = mock(ScoreDirector.class);
        List<TestdataEntity> selectionList = new ArrayList<>();
        selectionList.add(new TestdataEntity("C"));
        selectionList.add(new TestdataEntity("A"));
        selectionList.add(new TestdataEntity("D"));
        selectionList.add(new TestdataEntity("B"));
        selectionSorter.sort(scoreDirector, selectionList);
        assertCodesOfIterator(selectionList.iterator(), "D", "C", "B", "A");
    }

}
