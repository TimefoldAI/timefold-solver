package ai.timefold.solver.service.definition.api.validation;

import java.util.List;

import ai.timefold.solver.service.definition.impl.validation.AbstractLegacyIssue;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class ValidationIssueSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serializeValidationResult() throws JsonProcessingException {
        ValidationBuilder builder = new ValidationBuilder().addIssue(new TestIssue("entityId"));

        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(builder.build());

        final String expected = """
                {
                  "status" : "ERRORS",
                  "issues" : [ {
                    "code" : "MalformedEntityIssue",
                    "severity" : "ERROR",
                    "entityId" : "entityId"
                  } ]
                }""";

        Assertions.assertThat(json).isEqualToNormalizingNewlines(expected);
    }

    @Test
    void serializeLegacyValidationResultWithoutLegacyMessage() throws JsonProcessingException {
        ValidationBuilder builder = new ValidationBuilder().addIssue(new TestIssue("entityId"));

        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(builder.buildLegacyValidationResult());

        final String expected = """
                {
                  "summary" : "ERRORS",
                  "errors" : [ "Validation error of code (MalformedEntityIssue) occurred." ]
                }""";

        Assertions.assertThat(json).isEqualToNormalizingNewlines(expected);
    }

    @Test
    void serializeLegacyValidationResult() throws JsonProcessingException {
        ValidationBuilder builder = new ValidationBuilder()
                .addIssue(new TestLegacyIssue("entityId", "Entity (entityId) is malformed."));

        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(builder.buildLegacyValidationResult());

        final String expected = """
                {
                  "summary" : "ERRORS",
                  "errors" : [ "Entity (entityId) is malformed." ]
                }""";

        Assertions.assertThat(json).isEqualToNormalizingNewlines(expected);
    }

    @Test
    void serializeValidationIssueType() throws JsonProcessingException {
        IssueType issueType =
                new IssueType(IssueCode.of("MalformedEntityIssue"), IssueSeverity.ERROR, "Entity (entityId) is malformed.");

        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(issueType);

        final String expected = """
                {
                  "code" : "MalformedEntityIssue",
                  "severity" : "ERROR",
                  "metadata" : [ {
                    "message" : "Entity (entityId) is malformed.",
                    "type" : "Message"
                  } ]
                }""";

        Assertions.assertThat(json).isEqualToNormalizingNewlines(expected);
    }

    private static class TestIssue extends AbstractIssue {

        private final String entityId;

        private TestIssue(String entityId) {
            super(IssueCode.of("MalformedEntityIssue"), IssueSeverity.ERROR, List.of());
            this.entityId = entityId;
        }
    }

    private static class TestLegacyIssue extends AbstractLegacyIssue {

        private final String entityId;

        private TestLegacyIssue(String entityId, String legacyMessage) {
            super(IssueCode.of("MalformedEntityIssue"), IssueSeverity.ERROR, List.of(), legacyMessage);
            this.entityId = entityId;
        }
    }
}
