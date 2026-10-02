package ai.timefold.solver.service.definition.api;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class ModelMaturityLevelTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserializeExampleAsTemplate() throws JsonProcessingException {
        var level = mapper.readValue("\"Example\"", ModelMaturityLevel.class);
        Assertions.assertThat(level).isEqualTo(ModelMaturityLevel.Template);
    }

    @Test
    void deserializeTemplateAsTemplate() throws JsonProcessingException {
        var level = mapper.readValue("\"Template\"", ModelMaturityLevel.class);
        Assertions.assertThat(level).isEqualTo(ModelMaturityLevel.Template);
    }
}
