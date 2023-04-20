package ai.timefold.solver.core.impl.heuristic.selector.common.decorator;

import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertCodesOfIterator;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.config.heuristic.selector.common.decorator.SelectionSorterOrder;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.junit.jupiter.api.Test;

class WeightFactorySelectionSorterTest {

    @Test
    void sortAscending() {
        SelectionSorterWeightFactory<TestdataSolution, TestdataEntity> weightFactory = (solution, selection) -> Integer
                .valueOf(selection.getCode().charAt(0));
        WeightFactorySelectionSorter<TestdataSolution, TestdataEntity> selectionSorter = new WeightFactorySelectionSorter<>(
                weightFactory, SelectionSorterOrder.ASCENDING);
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
        SelectionSorterWeightFactory<TestdataSolution, TestdataEntity> weightFactory = (solution, selection) -> Integer
                .valueOf(selection.getCode().charAt(0));
        WeightFactorySelectionSorter<TestdataSolution, TestdataEntity> selectionSorter = new WeightFactorySelectionSorter<>(
                weightFactory, SelectionSorterOrder.DESCENDING);
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
