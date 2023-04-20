package ai.timefold.solver.examples.nqueens.optional.score;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.examples.nqueens.domain.NQueens;
import ai.timefold.solver.examples.nqueens.domain.Queen;

public class NQueensMapBasedEasyScoreCalculator implements EasyScoreCalculator<NQueens, SimpleScore> {

    @Override
    public SimpleScore calculateScore(NQueens nQueens) {
        int n = nQueens.getN();
        List<Queen> queenList = nQueens.getQueenList();

        Map<Integer, Integer> rowIndexCountMap = new HashMap<>(n);
        Map<Integer, Integer> ascendingDiagonalIndexCountMap = new HashMap<>(n);
        Map<Integer, Integer> descendingDiagonalIndexCountMap = new HashMap<>(n);
        int score = 0;
        for (Queen queen : queenList) {
            if (queen.getRow() != null) {
                int rowIndex = queen.getRowIndex();
                Integer rowIndexCount = rowIndexCountMap.get(rowIndex);
                if (rowIndexCount != null) {
                    score -= rowIndexCount;
                    rowIndexCount++;
                } else {
                    rowIndexCount = 1;
                }
                rowIndexCountMap.put(rowIndex, rowIndexCount);

                int ascendingDiagonalIndex = queen.getAscendingDiagonalIndex();
                Integer ascendingDiagonalIndexCount = ascendingDiagonalIndexCountMap.get(ascendingDiagonalIndex);
                if (ascendingDiagonalIndexCount != null) {
                    score -= ascendingDiagonalIndexCount;
                    ascendingDiagonalIndexCount++;
                } else {
                    ascendingDiagonalIndexCount = 1;
                }
                ascendingDiagonalIndexCountMap.put(ascendingDiagonalIndex, ascendingDiagonalIndexCount);

                int descendingDiagonalIndex = queen.getDescendingDiagonalIndex();
                Integer descendingDiagonalIndexCount = descendingDiagonalIndexCountMap.get(descendingDiagonalIndex);
                if (descendingDiagonalIndexCount != null) {
                    score -= descendingDiagonalIndexCount;
                    descendingDiagonalIndexCount++;
                } else {
                    descendingDiagonalIndexCount = 1;
                }
                descendingDiagonalIndexCountMap.put(descendingDiagonalIndex, descendingDiagonalIndexCount);
            }
        }
        return SimpleScore.of(score);
    }

}
