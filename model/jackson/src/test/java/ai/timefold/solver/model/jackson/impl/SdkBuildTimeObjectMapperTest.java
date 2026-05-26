package ai.timefold.solver.model.jackson.impl;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class SdkBuildTimeObjectMapperTest {

    private ObjectMapper mapper = SdkBuildTimeObjectMapperFactory.create();

    private record Simple(String id) {
    }

    private record DateTimeWrapper(OffsetDateTime dateTime) {
    }

    private record DurationWrapper(Duration duration) {
    }

    @Test
    void deserializationOfUnknownProperties_throwsException() {
        String wrongJson = """
                {
                  "id": "123",
                  "unknownProperty": "shouldFail"
                }
                """;

        Assertions.assertThatThrownBy(() -> mapper.readValue(wrongJson, Simple.class))
                .isInstanceOf(JsonProcessingException.class)
                .hasMessageContaining("unknownProperty");
    }

    @Test
    void serializeNonNullProperties_onlyIncludesNonNull() throws JsonProcessingException {
        Simple simple = new Simple(null);
        String json = mapper.writeValueAsString(simple);
        Assertions.assertThat(json).isEqualTo("{}");
    }

    @Test
    void serializeAndDeserializeDateTime_preservesTimeZone() throws JsonProcessingException {
        String json = """
                {
                  "dateTime": "2024-06-01T12:00:00+02:00"
                }
                """;

        DateTimeWrapper dto = mapper.readValue(json, DateTimeWrapper.class);
        String serialized = mapper.writeValueAsString(dto);

        Assertions.assertThat(serialized).contains("\"dateTime\":\"2024-06-01T12:00:00+02:00\"");
    }

    @Test
    void serializeOffsetDateTimeAsString() throws JsonProcessingException {
        DateTimeWrapper dto = new DateTimeWrapper(OffsetDateTime.of(2025, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        String serialized = mapper.writeValueAsString(dto);

        String expectedJson = """
                {
                  "dateTime": "2025-01-01T10:00:00Z"
                }
                """;

        Assertions.assertThat(serialized).isEqualToIgnoringWhitespace(expectedJson);
    }

    @Test
    void serializeDurationAsString() throws JsonProcessingException {
        DurationWrapper dto = new DurationWrapper(Duration.ofHours(5).plusMinutes(30));
        String serialized = mapper.writeValueAsString(dto);

        String expectedJson = """
                {
                  "duration": "PT5H30M"
                }
                """;

        Assertions.assertThat(serialized).isEqualToIgnoringWhitespace(expectedJson);
    }
}
