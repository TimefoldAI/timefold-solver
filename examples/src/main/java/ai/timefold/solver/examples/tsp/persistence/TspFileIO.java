package ai.timefold.solver.examples.tsp.persistence;

import java.io.File;

import ai.timefold.solver.examples.tsp.domain.TspSolution;
import ai.timefold.solver.persistence.common.api.domain.solution.SolutionFileIO;

public class TspFileIO implements SolutionFileIO<TspSolution> {

    private TspImporter importer = new TspImporter();
    private TspExporter exporter = new TspExporter();

    @Override
    public String getInputFileExtension() {
        return TspImporter.INPUT_FILE_SUFFIX;
    }

    @Override
    public String getOutputFileExtension() {
        return TspExporter.OUTPUT_FILE_SUFFIX;
    }

    @Override
    public TspSolution read(File inputSolutionFile) {
        return importer.readSolution(inputSolutionFile);
    }

    @Override
    public void write(TspSolution solution, File outputSolutionFile) {
        exporter.writeSolution(solution, outputSolutionFile);
    }

}
