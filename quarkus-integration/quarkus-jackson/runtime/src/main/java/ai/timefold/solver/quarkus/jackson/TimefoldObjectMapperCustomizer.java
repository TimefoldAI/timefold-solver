package ai.timefold.solver.quarkus.jackson;

import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.jackson.ObjectMapperCustomizer;

/**
 * OptaPlanner doesn't use Jackson, but it does have optional Jackson support for serializing the score etc.
 */
@Singleton
public class TimefoldObjectMapperCustomizer implements ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper objectMapper) {
        objectMapper.findAndRegisterModules(); // Loads OptaPlanner Jackson module via ServiceLoader.
    }

}
