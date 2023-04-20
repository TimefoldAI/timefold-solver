package ai.timefold.solver.quarkus.devui;

import java.util.List;

public class TimefoldDevUIProperties {
    private final TimefoldModelProperties timefoldModelProperties;
    private final String effectiveSolverConfigXML;
    private final List<String> constraintList;

    public TimefoldDevUIProperties(TimefoldModelProperties timefoldModelProperties, String effectiveSolverConfigXML,
            List<String> constraintList) {
        this.timefoldModelProperties = timefoldModelProperties;
        this.effectiveSolverConfigXML = effectiveSolverConfigXML;
        this.constraintList = constraintList;
    }

    public TimefoldModelProperties getTimefoldModelProperties() {
        return timefoldModelProperties;
    }

    public String getEffectiveSolverConfig() {
        return effectiveSolverConfigXML;
    }

    public List<String> getConstraintList() {
        return constraintList;
    }
}
