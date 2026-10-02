package ai.timefold.solver.core.testdomain.shadow.shared_source;

import ai.timefold.solver.core.testdomain.TestdataObject;

public class TestdataSharedSourceValue extends TestdataObject {

    int startTime;
    int duration;

    public TestdataSharedSourceValue() {
    }

    public TestdataSharedSourceValue(String code, int startTime, int duration) {
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
