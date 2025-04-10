package ai.timefold.solver.core.impl.testdata.domain.invalid.duplicateweightconfiguration;

import java.util.List;

import ai.timefold.solver.core.api.domain.constraintweight.ConstraintConfiguration;
import ai.timefold.solver.core.api.domain.constraintweight.ConstraintConfigurationProvider;
import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;

@PlanningSolution
public class TestdataDuplicateWeightConfigurationSolution {

    public static SolutionDescriptor<TestdataDuplicateWeightConfigurationSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataDuplicateWeightConfigurationSolution.class);
    }

    @ConstraintConfigurationProvider
    private ConstraintConfiguration configuration;
    private ConstraintWeightOverrides<SimpleScore> constraintWeightOverrides;

    @PlanningEntityCollectionProperty
    private List<TestdataEntity> entityList;

    @PlanningScore
    private SimpleScore score;
}
