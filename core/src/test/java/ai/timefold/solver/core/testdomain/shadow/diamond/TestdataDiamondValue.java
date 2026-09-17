package ai.timefold.solver.core.testdomain.shadow.diamond;

import ai.timefold.solver.core.testdomain.TestdataObject;

public class TestdataDiamondValue extends TestdataObject {

    int startTime;
    int duration;

    public TestdataDiamondValue() {
    }

    public TestdataDiamondValue(String code, int startTime, int duration) {
        super(code);
        this.startTime = startTime;
        this.duration = duration;
    }

    public int getStartTime() {
        return startTime;
    }

    public int getDuration() {
        return duration;
    }

}
