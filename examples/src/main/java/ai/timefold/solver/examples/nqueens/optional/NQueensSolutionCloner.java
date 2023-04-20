package ai.timefold.solver.examples.nqueens.optional;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.cloner.SolutionCloner;
import ai.timefold.solver.examples.nqueens.domain.NQueens;
import ai.timefold.solver.examples.nqueens.domain.Queen;

public class NQueensSolutionCloner implements SolutionCloner<NQueens> {

    @Override
    public NQueens cloneSolution(NQueens original) {
        NQueens clone = new NQueens(original.getId());
        clone.setN(original.getN());
        clone.setColumnList(original.getColumnList());
        clone.setRowList(original.getRowList());
        List<Queen> queenList = original.getQueenList();
        List<Queen> clonedQueenList = new ArrayList<Queen>(queenList.size());
        for (Queen originalQueen : queenList) {
            Queen cloneQueen = new Queen(originalQueen.getId());
            cloneQueen.setColumn(originalQueen.getColumn());
            cloneQueen.setRow(originalQueen.getRow());
            clonedQueenList.add(cloneQueen);
        }
        clone.setQueenList(clonedQueenList);
        clone.setScore(original.getScore());
        return clone;
    }

}
