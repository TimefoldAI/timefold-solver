package ai.timefold.solver.quarkus.testdomain.suppliervariable.list;

import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowSources;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowVariableLooped;

@PlanningEntity
public class TestdataQuarkusSupplierVariableListValue {
    String name;

    private List<TestdataQuarkusSupplierVariableListValue> dependencies;

    @PreviousElementShadowVariable(sourceVariableName = "values")
    private TestdataQuarkusSupplierVariableListValue previous;

    @ShadowVariable(supplierName = "startTimeSupplier")
    private Integer startTime;

    @ShadowVariableLooped
    private boolean looped;

    public TestdataQuarkusSupplierVariableListValue() {
    }

    public TestdataQuarkusSupplierVariableListValue(String name) {
        this.name = name;
    }

    public TestdataQuarkusSupplierVariableListValue(String name, List<TestdataQuarkusSupplierVariableListValue> dependencies) {
        this.name = name;
        this.dependencies = dependencies;
    }

    public Integer getStartTime() {
        return startTime;
    }

    public void setStartTime(Integer startTime) {
        this.startTime = startTime;
    }

    public boolean isLooped() {
        return looped;
    }

    public void setLooped(boolean looped) {
        this.looped = looped;
    }

    @ShadowSources({ "previous.startTime", "dependencies[].startTime" })
    private Integer startTimeSupplier() {
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
