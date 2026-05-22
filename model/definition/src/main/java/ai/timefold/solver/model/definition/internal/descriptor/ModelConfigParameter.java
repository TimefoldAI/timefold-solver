package ai.timefold.solver.model.definition.internal.descriptor;

import ai.timefold.solver.model.definition.api.ModelConfigOverrides;
import ai.timefold.solver.model.definition.api.domain.DataFormat;

import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;

/**
 * Represents a constraint parameter, which is the constraint weight or any other parameter impacting the constraint behavior.
 * <p>
 * Parameters are defined by the model in the {@link ModelConfigOverrides} class.
 *
 * @param id the parameter id
 * @param name the human-readable name
 * @param description the parameter human-readable description
 * @param kind the parameter kind
 * @param type the OpenAPI schema type
 * @param arrayItemType the OpenAPI schema type for array items
 * @param format the data format which further specifies the schema type
 * @param schemaTypeRef the OpenAPI schema type reference (used when the schema type is not a primitive type)
 * @param constraintNameRef the constraint name reference (used when the parameter is a constraint weight or any other parameter
 *        related to a constraint)
 */
public record ModelConfigParameter(
        String id,
        String name,
        String description,
        ParameterKind kind,
        SchemaType type,
        SchemaType arrayItemType,
        DataFormat format,
        String schemaTypeRef,
        String constraintNameRef) {

}
