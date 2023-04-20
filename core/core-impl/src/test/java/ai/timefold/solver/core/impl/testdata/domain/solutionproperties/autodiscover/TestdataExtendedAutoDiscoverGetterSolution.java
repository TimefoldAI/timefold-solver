package ai.timefold.solver.core.impl.testdata.domain.solutionproperties.autodiscover;

import java.util.List;

import ai.timefold.solver.core.api.domain.autodiscover.AutoDiscoverMemberType;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

@PlanningSolution(autoDiscoverMemberType = AutoDiscoverMemberType.GETTER)
public class TestdataExtendedAutoDiscoverGetterSolution extends TestdataAutoDiscoverGetterSolution {

    public static SolutionDescriptor<TestdataExtendedAutoDiscoverGetterSolution> buildSubclassSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataExtendedAutoDiscoverGetterSolution.class,
                TestdataEntity.class);
    }

    private TestdataObject singleProblemFactFieldOverride;
    private List<TestdataValue> problemFactListFieldOverride;

    private List<TestdataEntity> entityListFieldOverride;
    private TestdataEntity otherEntityFieldOverride;

    public TestdataExtendedAutoDiscoverGetterSolution() {
    }

    public TestdataExtendedAutoDiscoverGetterSolution(String code) {
        super(code);
    }

    public TestdataExtendedAutoDiscoverGetterSolution(String code, TestdataObject singleProblemFact,
            List<TestdataValue> problemFactList, List<TestdataEntity> entityList,
            TestdataEntity otherEntity) {
        super(code);
        this.singleProblemFactFieldOverride = singleProblemFact;
        this.problemFactListFieldOverride = problemFactList;
        this.entityListFieldOverride = entityList;
        this.otherEntityFieldOverride = otherEntity;
    }

    @Override
    public TestdataObject getSingleProblemFact() {
        return singleProblemFactFieldOverride;
    }

    @ProblemFactProperty // Override from a fact collection to a single fact
    @ValueRangeProvider(id = "valueRange")
    @Override
    public List<TestdataValue> getProblemFactList() {
        return problemFactListFieldOverride;
    }

    @Override
    public List<TestdataEntity> getEntityList() {
        return entityListFieldOverride;
    }

    @Override
    public TestdataEntity getOtherEntity() {
        return otherEntityFieldOverride;
    }

}
