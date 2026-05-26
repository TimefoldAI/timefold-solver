package ai.timefold.solver.model.quarkus.deployment.testdata.storage;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.model.definition.api.ModelConvertor;
import ai.timefold.solver.model.definition.api.domain.ModelConfig;

@ApplicationScoped
public class TestdataModelConvertor implements
        ModelConvertor<SimpleScore, TestdataModelInput, TestdataModelConfig, TestdataSolution, TestdataModelOutput> {

    @Override
    public TestdataSolution toSolverModel(TestdataModelInput modelInput, ModelConfig<TestdataModelConfig> modelConfig,
            Optional<TestdataModelOutput> lastModelOutput) {
        return new TestdataSolution();
    }

    @Override
    public TestdataModelOutput toModelOutput(TestdataSolution solverModel) {
        return new TestdataModelOutput();
    }

    @Override
    public TestdataModelInput applyOutputToInput(TestdataModelInput modelInput, TestdataModelOutput modelOutput) {
        return modelInput;
    }
}
