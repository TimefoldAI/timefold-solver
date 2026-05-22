package ai.timefold.solver.model.quarkus.deployment.testdata.validationduplicates;

import jakarta.enterprise.context.ApplicationScoped;

import ai.timefold.solver.model.definition.api.domain.ModelConfig;
import ai.timefold.solver.model.definition.api.validation.ModelValidator;
import ai.timefold.solver.model.definition.api.validation.ValidationBuilder;
import ai.timefold.solver.model.quarkus.deployment.defaults.EmptyModelConfigOverrides;

/**
 * Simulates duplicate issue types (by their code) in the catalog.
 */
@ApplicationScoped
public class TestdataModelValidator
        implements ModelValidator<TestdataSolution, EmptyModelConfigOverrides> {

    @Override
    public void validate(ValidationBuilder validationBuilder, TestdataSolution modelInput,
            ModelConfig<EmptyModelConfigOverrides> modelConfig) {
    }
}
