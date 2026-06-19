package ai.timefold.solver.service.quarkus.deployment.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.service.jackson.impl.SdkBuildTimeObjectMapperFactory;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class SchemaPostProcessorTest {

    private final ObjectMapper objectMapper = SdkBuildTimeObjectMapperFactory.create();

    @Test
    void testRemoveDiscriminators() throws JsonProcessingException {
        final String schemaDefinitionString = """
                      {
                        "required" : [ "type" ],
                        "type" : "object",
                        "properties" : {
                          "type" : {
                            "description" : "The concrete type of this GeoJson object.",
                            "type" : "string",
                            "allOf" : [ {
                              "$ref" : "#/components/schemas/GeometryType"
                            } ]
                          }
                        },
                        "oneOf" : [ {
                          "$ref" : "#/components/schemas/Point"
                        }, {
                          "$ref" : "#/components/schemas/GeoJsonAreaGeometry"
                        } ],
                        "discriminator" : {
                          "propertyName" : "type",
                          "mapping" : {
                            "Point" : "#/components/schemas/Point",
                            "Polygon" : "#/components/schemas/Polygon",
                            "MultiPolygon" : "#/components/schemas/MultiPolygon"
                          }
                        }
                      }

                """;
        JsonNode schemaDefinition = objectMapper.readTree(schemaDefinitionString);
        JsonNode processedSchema = SchemaPostProcessor.removeDiscriminators(schemaDefinition);

        final String expectedSchemaString = """
                      {
                        "required" : [ "type" ],
                        "type" : "object",
                        "properties" : {
                          "type" : {
                            "description" : "The concrete type of this GeoJson object.",
                            "type" : "string",
                            "allOf" : [ {
                              "$ref" : "#/components/schemas/GeometryType"
                            } ]
                          }
                        },
                        "oneOf" : [ {
                          "$ref" : "#/components/schemas/Point"
                        }, {
                          "$ref" : "#/components/schemas/GeoJsonAreaGeometry"
                        } ]
                      }
                """;
        JsonNode expectedSchema = objectMapper.readTree(expectedSchemaString);
        assertThat(processedSchema).isEqualTo(expectedSchema);
    }
}
