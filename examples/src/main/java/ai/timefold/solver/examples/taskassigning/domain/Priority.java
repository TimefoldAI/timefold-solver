package ai.timefold.solver.examples.taskassigning.domain;

import ai.timefold.solver.examples.common.swingui.components.Labeled;

public enum Priority implements Labeled {
    MINOR,
    MAJOR,
    CRITICAL;

    @Override
    public String getLabel() {
        switch (this) {
            case MINOR:
                return "Minor priority";
            case MAJOR:
                return "Major priority";
            case CRITICAL:
                return "Critical priority";
            default:
                throw new IllegalStateException("The priority (" + this + ") is not implemented.");
        }
    }

}
