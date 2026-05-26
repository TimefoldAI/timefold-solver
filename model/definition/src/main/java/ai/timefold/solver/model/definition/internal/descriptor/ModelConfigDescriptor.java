package ai.timefold.solver.model.definition.internal.descriptor;

import java.util.List;

/**
 * Describes a model configuration.
 *
 * @param schemaTypeRef the OpenAPI schema type reference
 * @param configParameters the list of model configuration parameters
 */
public record ModelConfigDescriptor(
        String schemaTypeRef,
        List<ModelConfigParameter> configParameters) {
}
