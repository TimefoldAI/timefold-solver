package ai.timefold.solver.examples.nqueens.persistence;

import ai.timefold.solver.examples.common.persistence.AbstractJsonSolutionFileIO;
import ai.timefold.solver.examples.nqueens.domain.NQueens;

public class NQueensSolutionFileIO extends AbstractJsonSolutionFileIO<NQueens> {

    public NQueensSolutionFileIO() {
        super(NQueens.class);
    }
}
