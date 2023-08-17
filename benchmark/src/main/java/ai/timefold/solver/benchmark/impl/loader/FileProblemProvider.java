package ai.timefold.solver.benchmark.impl.loader;

import java.io.File;
import java.util.Objects;

import jakarta.xml.bind.annotation.XmlTransient;

import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;
import ai.timefold.solver.persistence.common.api.domain.solution.SolutionFileIO;

public class FileProblemProvider<Solution_> implements ProblemProvider<Solution_> {

    @XmlTransient
    private SolutionFileIO<Solution_> solutionFileIO;

    private File problemFile;

    private FileProblemProvider() {
        // Required by JAXB
    }

    public FileProblemProvider(SolutionFileIO<Solution_> solutionFileIO, File problemFile) {
        this.solutionFileIO = solutionFileIO;
        this.problemFile = problemFile;
    }

    public SolutionFileIO<Solution_> getSolutionFileIO() {
        return solutionFileIO;
    }

    @Override
    public String getProblemName() {
        String name = problemFile.getName();
        int lastDotIndex = name.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return name.substring(0, lastDotIndex);
        } else {
            return name;
        }
    }

    @Override
    public Solution_ readProblem() {
        return solutionFileIO.read(problemFile);
    }

    @Override
    public void writeSolution(Solution_ solution, SubSingleBenchmarkResult subSingleBenchmarkResult) {
        String filename = subSingleBenchmarkResult.getSingleBenchmarkResult().getProblemBenchmarkResult().getName()
                + "." + solutionFileIO.getOutputFileExtension();
        File solutionFile = new File(subSingleBenchmarkResult.getResultDirectory(), filename);
        solutionFileIO.write(solution, solutionFile);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        FileProblemProvider<?> that = (FileProblemProvider<?>) other;
        return Objects.equals(problemFile, that.problemFile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(problemFile);
    }

    @Override
    public String toString() {
        return problemFile.toString();
    }

}
