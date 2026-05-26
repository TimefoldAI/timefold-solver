package ai.timefold.solver.model.definition.api.validation;

import ai.timefold.solver.model.definition.api.ModelConfigOverrides;
import ai.timefold.solver.model.definition.api.ModelInput;
import ai.timefold.solver.model.definition.api.domain.ModelConfig;

/**
 * Provides a complex validation of the model input before the solving, or other operation, starts.
 *
 * @param <ModelInput_> the type of the model input
 */
public interface ModelValidator<ModelInput_ extends ModelInput, ModelConfigOverrides_ extends ModelConfigOverrides> {

    /**
     * Validates the model input before the solving, or other operation, starts.
     *
     * @param validationBuilder {@link ValidationBuilder} instance to add validation issues to; never null
     * @param modelInput the model input; never null
     * @param modelConfig the model configuration; never null
     */
    default void validate(ValidationBuilder validationBuilder, ModelInput_ modelInput,
            ModelConfig<ModelConfigOverrides_> modelConfig) {
        validationBuilder.unsupported();
    }
}
