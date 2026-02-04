package ai.timefold.solver.quarkus.testdomain.declarative.list;

import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariablesInconsistent;

@PlanningEntity
public class TestdataQuarkusDeclarativeShadowVariableListValue {
    String name;

    private List<TestdataQuarkusDeclarativeShadowVariableListValue> dependencies;

    @PreviousElementShadowVariable(sourceVariableName = "values")
    private TestdataQuarkusDeclarativeShadowVariableListValue previous;

    @ShadowVariable(supplierName = "startTimeSupplier")
    private Integer startTime;

    @ShadowVariablesInconsistent
    private boolean inconsistent;

    public TestdataQuarkusDeclarativeShadowVariableListValue() {
    }

    public TestdataQuarkusDeclarativeShadowVariableListValue(String name) {
        this.name = name;
    }

    public TestdataQuarkusDeclarativeShadowVariableListValue(String name,
            List<TestdataQuarkusDeclarativeShadowVariableListValue> dependencies) {
        this.name = name;
        this.dependencies = dependencies;
    }

    public Integer getStartTime() {
        return startTime;
    }

    public void setStartTime(Integer startTime) {
        this.startTime = startTime;
    }

    public boolean isInconsistent() {
        return inconsistent;
    }

    public void setInconsistent(boolean inconsistent) {
        this.inconsistent = inconsistent;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<TestdataQuarkusDeclarativeShadowVariableListValue> getDependencies() {
        return dependencies;
    }

    public void setDependencies(
            List<TestdataQuarkusDeclarativeShadowVariableListValue> dependencies) {
        this.dependencies = dependencies;
    }

    public TestdataQuarkusDeclarativeShadowVariableListValue getPrevious() {
        return previous;
    }

    public void setPrevious(
            TestdataQuarkusDeclarativeShadowVariableListValue previous) {
        this.previous = previous;
    }

    @ShadowSources({ "previous.startTime", "dependencies[].startTime" })
    public Integer startTimeSupplier() {
        if (previous == null) {
            return 0;
        }
        int minStartTime = previous.getStartTime();
        if (dependencies != null) {
            for (var dependency : dependencies) {
                if (dependency.getStartTime() > minStartTime) {
                    minStartTime = dependency.getStartTime();
                }
            }
        }
        return minStartTime + 1;
    }

    public String toString() {
        return name + "(" + startTime + ")";
    }
}
