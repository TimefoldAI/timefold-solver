package ai.timefold.solver.model.json.impl.schema.ext;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.AbstractKeyword;
import com.networknt.schema.JsonNodePath;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaException;
import com.networknt.schema.JsonValidator;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.ValidationContext;

public class UniqueItemsWithPropertyKeyword extends AbstractKeyword {
    public UniqueItemsWithPropertyKeyword() {
        super("uniqueItems");
    }

    @Override
    public JsonValidator newValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode,
            JsonSchema parentSchema, ValidationContext validationContext) throws JsonSchemaException, Exception {
        return new UniqueItemsValidatorWithProperty(schemaLocation, evaluationPath, schemaNode, parentSchema,
                validationContext);
    }

}
