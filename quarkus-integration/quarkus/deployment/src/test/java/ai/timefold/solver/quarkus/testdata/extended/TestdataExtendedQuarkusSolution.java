package ai.timefold.solver.quarkus.testdata.extended;

import ai.timefold.solver.quarkus.testdata.normal.domain.TestdataQuarkusSolution;

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
