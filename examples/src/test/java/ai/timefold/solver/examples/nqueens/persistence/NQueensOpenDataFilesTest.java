package ai.timefold.solver.examples.nqueens.persistence;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.persistence.OpenDataFilesTest;
import ai.timefold.solver.examples.nqueens.app.NQueensApp;
import ai.timefold.solver.examples.nqueens.domain.NQueens;

class NQueensOpenDataFilesTest extends OpenDataFilesTest<NQueens> {

    @Override
    protected CommonApp<NQueens> createCommonApp() {
        return new NQueensApp();
    }
}
