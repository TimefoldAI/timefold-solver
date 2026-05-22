package ai.timefold.solver.model.json.internal.schema;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import ai.timefold.solver.model.definition.api.validation.Validator;
import ai.timefold.solver.model.json.api.schema.LocalDateTimeFormat;
import ai.timefold.solver.model.json.api.schema.OffsetDateTimeFormat;
import ai.timefold.solver.model.json.api.schema.ZoneIdFormat;
import ai.timefold.solver.model.json.impl.schema.ext.UniqueItemsWithPropertyKeyword;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonMetaSchema;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.NonValidationKeyword;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import io.quarkus.arc.DefaultBean;

@ApplicationScoped
@DefaultBean
public class JsonSchemaValidator implements Validator<ObjectNode> {

    protected ObjectMapper mapper = new ObjectMapper();
    protected JsonSchemaFactory validatorFactory;
    protected SchemaValidatorsConfig schemaValidatorsConfig;

    private Map<String, JsonSchema> schemasByModelId = new ConcurrentHashMap<>();

    @PostConstruct
    public void setup() {

        final JsonMetaSchema overrideValidatorMetaSchema = JsonMetaSchema
                .builder("https://json-schema.org/draft-04/schema", JsonMetaSchema.getV4())
                .keyword(new NonValidationKeyword("deprecated"))
                .keyword(new NonValidationKeyword("example"))
                .keyword(new NonValidationKeyword("exclusiveMinimum"))
                .keyword(new NonValidationKeyword("externalDocs"))
                .keyword(new NonValidationKeyword("nullable"))
                .keyword(new NonValidationKeyword("readOnly"))
                .keyword(new NonValidationKeyword("x-uniqueItemsProperty"))
                .keyword(new UniqueItemsWithPropertyKeyword())
                // accept also LocalDateTime values, extending what default JSON Schema accepts
                .format(new LocalDateTimeFormat())
                .format(new OffsetDateTimeFormat())
                // validate also timezone identifiers
                .format(new ZoneIdFormat())
                .build();
        validatorFactory = JsonSchemaFactory
                .builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4)).jsonMapper(mapper)
                .metaSchema(overrideValidatorMetaSchema)
                .build();

        schemaValidatorsConfig = createSchemaValidatorsConfig();
    }

    private SchemaValidatorsConfig createSchemaValidatorsConfig() {
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        config.setTypeLoose(false);
        // allow negative duration: https://github.com/networknt/json-schema-validator/blob/master/doc/duration.md
        config.setStrict("duration", false);
        return config;
    }

    @Override
    public List<String> validate(UUID tenantId, String id, String version, String operation, String configurationId,
            ObjectNode entity) {
        if (entity == null) {
            return List.of("Body is required");
        }
        JsonSchema schema = null;
        String lookupKey = null;
        // first look up by tenant aware key to take into account registered models
        if (tenantId != null) {
            lookupKey = lookupKey(tenantId, id, version, operation);
            schema = schemasByModelId.get(lookupKey);
        }
        // if not found look up by model only key
        if (schema == null) {
            lookupKey = lookupKey(id, version, operation);
            schema = schemasByModelId.get(lookupKey);
        }
        // schema not cached yet, attempt to load it from resources/catalog
        if (schema == null) {
            JsonSchemaDesc desc = loadSchema(tenantId, id, version, operation);

            try (InputStream in = desc.content()) {
                if (in != null) {
                    schema = readSchema(in);

                    // once loaded schema, register it under all keys from descriptor
                    // keys can be model only for public/embedded models
                    // tenant aware keys for private and shared with models
                    for (String key : desc.registersWith()) {
                        schemasByModelId.put(key, schema);
                    }
                }
            } catch (IOException e) {
                schema = null;
            }
        }

        if (schema == null) {
            throw new IllegalStateException("No schema found for " + id + "_" + version + " and operation " + operation);
        }
        Set<ValidationMessage> errors = schema.validate(entity);
        return errors.stream().map(ValidationMessage::toString).collect(Collectors.toList());
    }

    List<String> validate(JsonNode entity, JsonSchema schema) {
        Set<ValidationMessage> errors = schema.validate(entity);
        return errors.stream().map(ValidationMessage::toString).collect(Collectors.toList());
    }

    JsonSchema readSchema(InputStream schemaDefinitionInputStream) throws IOException {
        JsonNode schemaDef = mapper.readTree(schemaDefinitionInputStream);
        return createSchema(schemaDef);
    }

    JsonSchema readSchema(String schemaDefinition) throws JsonProcessingException {
        JsonNode schemaDef = mapper.readTree(schemaDefinition);
        return createSchema(schemaDef);
    }

    private JsonSchema createSchema(JsonNode schemaDefinition) {
        return validatorFactory.getSchema(schemaDefinition, schemaValidatorsConfig);
    }

    protected String lookupKey(UUID tenantId, String id, String version, String operation) {
        return lookupKey(tenantId == null ? "" : tenantId.toString(), id, version, operation);
    }

    protected String lookupKey(String tenantId, String id, String version, String operation) {
        return tenantId + "_" + id + "_" + version + (operation == null ? "" : "_" + operation);
    }

    protected String lookupKey(String id, String version, String operation) {
        return id + "_" + version + (operation == null ? "" : "_" + operation);
    }

    /**
     * Loads JSON schema from resources.
     * <p>
     * This method is overridden in the platform to provide tenant-aware schema loading
     */
    protected JsonSchemaDesc loadSchema(@SuppressWarnings("unused") UUID tenantId, String id, String version,
            String operation) {
        return new JsonSchemaDesc(Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("/" + id + "_" + version + "/jsonschema/" + operation + ".json"),
                Set.of(lookupKey(id, version, operation)));
    }

    /**
     * Removes a JSON schema from a local cache.
     * <p>
     * This method is used by the platform to remove schemas when a registered model changes.
     */
    @SuppressWarnings("unused")
    protected void removeFromCache(String keyExpression) {

        List<String> keysToRemove = schemasByModelId.keySet().stream().filter(key -> key.matches(keyExpression)).toList();

        for (String key : keysToRemove) {
            schemasByModelId.remove(key);
        }
    }
}
