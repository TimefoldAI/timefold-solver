package ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigschema;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.service.definition.api.ModelConvertor;
import ai.timefold.solver.service.definition.api.domain.ModelConfig;
import ai.timefold.solver.service.quarkus.deployment.defaults.EmptyModelConfigOverrides;

@ApplicationScoped
public class TestdataModelConvertor implements
        ModelConvertor<SimpleScore, TestdataSolution, EmptyModelConfigOverrides, TestdataSolution, TestdataSolution> {

    @Override
    public TestdataSolution toSolverModel(TestdataSolution modelInput, ModelConfig<EmptyModelConfigOverrides> modelConfig,
            Optional<TestdataSolution> lastModelOutput) {
        return modelInput;
    }

    @Override
    public TestdataSolution toModelOutput(TestdataSolution solverModel) {
        return solverModel;
    }

    @Override
    public TestdataSolution applyOutputToInput(TestdataSolution modelInput, TestdataSolution modelOutput) {
        return modelInput;
    }
}
