package ai.timefold.solver.examples.common.persistence;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.File;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.app.LoggingTest;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public abstract class ImportDataFilesTest<Solution_> extends LoggingTest {

    protected abstract AbstractSolutionImporter<Solution_> createSolutionImporter();

    protected abstract String getDataDirName();

    protected Predicate<File> dataFileInclusionFilter() {
        return file -> true;
    }

    private static List<File> getInputFiles(String dataDirName, AbstractSolutionImporter<?> solutionImporter) {
        File importDir = new File(CommonApp.determineDataDir(dataDirName), "import");
        return getAllFilesRecursivelyAndSorted(importDir, solutionImporter::acceptInputFile);
    }

    @TestFactory
    @Execution(ExecutionMode.CONCURRENT)
    Stream<DynamicTest> readSolution() {
        AbstractSolutionImporter<Solution_> solutionImporter = createSolutionImporter();
        return getInputFiles(getDataDirName(), solutionImporter).stream()
                .filter(dataFileInclusionFilter())
                .map(importFile -> dynamicTest(
                        importFile.getName(),
                        () -> solutionImporter.readSolution(importFile)));
    }
}
