package ai.timefold.solver.model.json.impl.schema.ext;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.ExecutionContext;
import com.networknt.schema.JsonNodePath;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.UniqueItemsValidator;
import com.networknt.schema.ValidationContext;
import com.networknt.schema.ValidationMessage;

public class UniqueItemsValidatorWithProperty extends UniqueItemsValidator {

    private final boolean unique;
    private String propertyName;

    public UniqueItemsValidatorWithProperty(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode,
            JsonSchema parentSchema, ValidationContext validationContext) {
        super(schemaLocation, evaluationPath, schemaNode, parentSchema, validationContext);
        if (schemaNode.isBoolean()) {
            unique = schemaNode.booleanValue();
        } else {
            unique = false;
        }

        if (parentSchema.getSchemaNode().has("x-uniqueItemsProperty")) {
            this.propertyName = parentSchema.getSchemaNode().get("x-uniqueItemsProperty").asText();
        }
    }

    public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode,
            JsonNodePath instanceLocation) {

        if (unique) {
            Set<JsonNode> set = new HashSet<>();
            for (JsonNode n : node) {
                if (propertyName != null) {
                    n = n.get(propertyName);
                }
                if (!set.add(n)) {
                    return Collections.singleton(
                            message()
                                    .message("{0}: Array can only contain unique items"
                                            + (propertyName != null ? " identified by property '" + propertyName + "'" : "")
                                            + ", duplicates {1}")
                                    .arguments(n)
                                    .instanceNode(node).instanceLocation(instanceLocation)
                                    .locale(executionContext.getExecutionConfig().getLocale())
                                    .failFast(executionContext.isFailFast()).build());
                }

            }
        }

        return Collections.emptySet();
    }
}
