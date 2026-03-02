package ai.timefold.solver.benchmark.util;

import java.io.File;
import java.util.Arrays;

import ai.timefold.solver.core.api.domain.solution.SolutionFileIO;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;

public class RigidTestdataSolutionFileIO implements SolutionFileIO<TestdataSolution> {

    @Override
    public String getInputFileExtension() {
        return "txt";
    }

    @Override
    public TestdataSolution read(File inputSolutionFile) {
        TestdataSolution solution = new TestdataSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        solution.setEntityList(Arrays.asList(
                new TestdataEntity("e1"), new TestdataEntity("e2"), new TestdataEntity("e3")));
        return solution;
    }

    @Override
    public void write(TestdataSolution solution, File outputSolutionFile) {
        // Do nothing
    }

}
