package ai.timefold.solver.core.impl.testdata.domain.constraintconfiguration.extended;

import ai.timefold.solver.core.api.domain.constraintweight.ConstraintConfigurationProvider;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

@Deprecated(forRemoval = true, since = "1.13.0")
@PlanningSolution
public class TestdataExtendedConstraintConfigurationSolution extends TestdataSolution {

    public static SolutionDescriptor<TestdataExtendedConstraintConfigurationSolution> buildExtendedSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataExtendedConstraintConfigurationSolution.class,
                TestdataEntity.class);
    }

    private TestdataExtendedConstraintConfiguration constraintConfiguration;

    public TestdataExtendedConstraintConfigurationSolution() {
    }

    public TestdataExtendedConstraintConfigurationSolution(String code) {
        super(code);
    }

    @ConstraintConfigurationProvider
    public TestdataExtendedConstraintConfiguration getConstraintConfiguration() {
        return constraintConfiguration;
    }

    public void setConstraintConfiguration(TestdataExtendedConstraintConfiguration constraintConfiguration) {
        this.constraintConfiguration = constraintConfiguration;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

}
