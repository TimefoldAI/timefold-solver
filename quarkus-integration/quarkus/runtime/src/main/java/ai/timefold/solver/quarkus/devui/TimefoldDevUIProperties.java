package ai.timefold.solver.quarkus.devui;

import java.util.List;

public class TimefoldDevUIProperties {
    private final TimefoldModelProperties optaPlannerModelProperties;
    private final String effectiveSolverConfigXML;
    private final List<String> constraintList;

    public TimefoldDevUIProperties(TimefoldModelProperties optaPlannerModelProperties, String effectiveSolverConfigXML,
            List<String> constraintList) {
        this.optaPlannerModelProperties = optaPlannerModelProperties;
        this.effectiveSolverConfigXML = effectiveSolverConfigXML;
        this.constraintList = constraintList;
    }

    public TimefoldModelProperties getOptaPlannerModelProperties() {
        return optaPlannerModelProperties;
    }

    public String getEffectiveSolverConfig() {
        return effectiveSolverConfigXML;
    }

    public List<String> getConstraintList() {
        return constraintList;
    }
}
