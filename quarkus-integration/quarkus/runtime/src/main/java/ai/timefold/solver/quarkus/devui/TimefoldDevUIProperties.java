package ai.timefold.solver.quarkus.devui;

import java.util.List;

import ai.timefold.solver.core.api.score.constraint.ConstraintRef;

public class TimefoldDevUIProperties { // TODO make record?

    private final TimefoldModelProperties timefoldModelProperties;
    private final String effectiveSolverConfigXML;
    private final List<ConstraintRef> constraintList;

    public TimefoldDevUIProperties(TimefoldModelProperties timefoldModelProperties, String effectiveSolverConfigXML,
            List<ConstraintRef> constraintList) {
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

    public List<ConstraintRef> getConstraintList() {
        return constraintList;
    }
}
