package ai.timefold.solver.examples.common.app;

import java.io.File;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.persistence.common.api.domain.solution.SolutionFileIO;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public abstract class UnsolvedDirSolveAllTurtleTest<Solution_> extends SolveAllTurtleTest<Solution_> {

    @Override
    protected List<File> getSolutionFiles(CommonApp<Solution_> commonApp) {
        File dataDir = CommonApp.determineDataDir(commonApp.getDataDirName());
        File unsolvedDataDir = new File(dataDir, "unsolved");
        if (!unsolvedDataDir.exists()) {
            throw new IllegalStateException("The directory unsolvedDataDir (" + unsolvedDataDir.getAbsolutePath()
                    + ") does not exist.");
        } else {
            String inputFileExtension = commonApp.createSolutionFileIO().getInputFileExtension();
            return getAllFilesRecursivelyAndSorted(unsolvedDataDir, file -> file.getName().endsWith(inputFileExtension));
        }
    }

    @Override
    protected ProblemFactory<Solution_> createProblemFactory(CommonApp<Solution_> commonApp) {
        SolutionFileIO<Solution_> solutionFileIO = commonApp.createSolutionFileIO();
        return (dataFile) -> {
            Solution_ problem = solutionFileIO.read(dataFile);
            logger.info("Opened: {}", dataFile);
            return problem;
        };
    }
}
