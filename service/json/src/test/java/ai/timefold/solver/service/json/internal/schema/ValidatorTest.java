package ai.timefold.solver.service.json.internal.schema;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.networknt.schema.JsonSchema;

class ValidatorTest {

    private static final String ISO_8601_OFFSET_DATE_TIME_SCHEMA = """
            {
              "description": "ISO 8601 datetime with offset to UTC)",
              "type": "string",
              "allOf":[
                {
                  "format":"date-time",
                  "type":"string"
                }
              ]
            }
            """;

    private JsonSchemaValidator validation;

    @BeforeEach
    void setup() {
        validation = new JsonSchemaValidator();
        validation.setup();
    }

    @Test
    void validOffsetDateTimeFormat() throws JsonProcessingException {
        JsonSchema schema = validation.readSchema(ISO_8601_OFFSET_DATE_TIME_SCHEMA);

        final String validOffsetDateTimeString = "2027-02-01T17:00:00Z";
        JsonNode payload = JsonNodeFactory.instance.textNode(validOffsetDateTimeString);

        List<String> errors = validation.validate(payload, schema);
        assertThat(errors).isEmpty();
    }

    @Test
    void invalidOffsetDateTimeFormat() throws JsonProcessingException {
        JsonSchema schema = validation.readSchema(ISO_8601_OFFSET_DATE_TIME_SCHEMA);

        final String invalidOffsetDateTimeString = "2027-02-01 17:00:00Z"; // ISO 8601 invalid (missing 'T' between date and time)
        JsonNode payload = JsonNodeFactory.instance.textNode(invalidOffsetDateTimeString);

        List<String> errors = validation.validate(payload, schema);
        assertThat(errors).hasSize(1).containsOnly(
                "$: does not match the date-time pattern must be a valid ISO-8601 date and time with an offset");
    }
}
