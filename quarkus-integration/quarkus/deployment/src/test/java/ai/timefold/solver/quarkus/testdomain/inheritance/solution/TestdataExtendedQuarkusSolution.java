package ai.timefold.solver.quarkus.testdomain.inheritance.solution;

import ai.timefold.solver.quarkus.testdomain.normal.TestdataQuarkusSolution;

public class TestdataExtendedQuarkusSolution extends TestdataQuarkusSolution {
    private String extraData;

    public TestdataExtendedQuarkusSolution() {
    }

    public TestdataExtendedQuarkusSolution(String extraData) {
        this.extraData = extraData;
    }

    public String getExtraData() {
        return extraData;
    }
}
