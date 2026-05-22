package ai.timefold.solver.model.quarkus.deployment.openapi;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SchemaPostProcessor {

    /**
     * Removes all 'discriminator' properties from a schema definition.
     * <p>
     * OpenAPI Schema does not require the 'discriminator' property (see
     * <a href='https://swagger.io/specification/v3/#discriminator-object'></a>).
     * The JSON Schema validator even logs a warning about an unrecognized keyword.
     *
     * @param schemaDef the JSON schema definition to process
     * @return the modified JSON schema definition without discriminator properties
     */
    public static JsonNode removeDiscriminators(JsonNode schemaDef) {
        List<JsonNode> discriminators = schemaDef.findParents("discriminator");

        for (JsonNode withDiscriminator : discriminators) {
            JsonNode discriminatorNode = withDiscriminator.get("discriminator");
            if (discriminatorNode.has("propertyName")) {
                ((ObjectNode) withDiscriminator).remove("discriminator");
            }
        }

        return schemaDef;
    }
}
