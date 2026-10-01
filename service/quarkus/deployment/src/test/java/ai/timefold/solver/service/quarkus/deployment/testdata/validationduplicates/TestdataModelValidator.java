package ai.timefold.solver.service.quarkus.deployment.testdata.validationduplicates;

import jakarta.enterprise.context.ApplicationScoped;

import ai.timefold.solver.service.definition.api.domain.ModelConfig;
import ai.timefold.solver.service.definition.api.validation.ModelValidator;
import ai.timefold.solver.service.definition.api.validation.ValidationBuilder;
import ai.timefold.solver.service.quarkus.deployment.defaults.EmptyModelConfigOverrides;

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
