package ai.timefold.solver.service.definition.api.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import ai.timefold.solver.service.definition.api.termination.SolverTerminationConfig;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class RunConfigurationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void convenienceConstructorsLeaveOptionsNull() {
        assertThat(new RunConfiguration("dataset", null, 4, Set.of("a")).options()).isNull();
        assertThat(new RunConfiguration("dataset", null).options()).isNull();
        assertThat(new RunConfiguration(4, null).options()).isNull();
        assertThat(new RunConfiguration("dataset").options()).isNull();
    }

    @Test
    void overrideFillsMissingOptionsFromFallback() {
        RunConfiguration primary = new RunConfiguration("dataset", null, null, Set.of(), null);
        RunConfiguration fallback = new RunConfiguration(null, null, null, Set.of(), Map.of("solver", "fast"));

        RunConfiguration merged = primary.override(fallback);

        assertThat(merged.options()).containsExactlyEntriesOf(Map.of("solver", "fast"));
    }

    @Test
    void overrideKeepsPrimaryOptionsWhenPresent() {
        RunConfiguration primary = new RunConfiguration("dataset", null, null, Set.of(), Map.of("solver", "accurate"));
        RunConfiguration fallback = new RunConfiguration(null, null, null, Set.of(), Map.of("solver", "fast"));

        RunConfiguration merged = primary.override(fallback);

        // Options are replaced wholesale, never merged key-by-key.
        assertThat(merged.options()).containsExactlyEntriesOf(Map.of("solver", "accurate"));
    }

    @Test
    void overrideKeepsPrimaryOptionsWhenPresentWithDisjointFallbackKeys() {
        RunConfiguration primary = new RunConfiguration("dataset", null, null, Set.of(), Map.of("solver", "accurate"));
        RunConfiguration fallback = new RunConfiguration(null, null, null, Set.of(), Map.of("logLevel", "debug"));

        RunConfiguration merged = primary.override(fallback);

        assertThat(merged.options()).containsExactlyEntriesOf(Map.of("solver", "accurate"));
    }

    @Test
    void overrideKeepsPrimaryEmptyOptionsInsteadOfInheriting() {
        RunConfiguration primary = new RunConfiguration("dataset", null, null, Set.of(), Map.of());
        RunConfiguration fallback = new RunConfiguration(null, null, null, Set.of(), Map.of("solver", "fast"));

        RunConfiguration merged = primary.override(fallback);

        // Only a null options map inherits from the fallback; an empty one is a deliberate "no options".
        assertThat(merged.options()).isEmpty();
    }

    @Test
    void overrideWithNullConfigurationKeepsOptions() {
        RunConfiguration primary = new RunConfiguration("dataset", null, null, Set.of(), Map.of("solver", "fast"));

        assertThat(primary.override(null).options()).containsExactlyEntriesOf(Map.of("solver", "fast"));
    }

    @Test
    void withTerminationPreservesOptions() {
        RunConfiguration configuration =
                new RunConfiguration("dataset", null, 4, Set.of("nightly"), Map.of("solver", "fast"));

        RunConfiguration copy = configuration.withTermination(new SolverTerminationConfig(Duration.ofMinutes(1), null));

        assertThat(copy.options()).containsExactlyEntriesOf(Map.of("solver", "fast"));
        assertThat(copy.termination().spentLimit()).isEqualTo(Duration.ofMinutes(1));
        assertThat(copy.name()).isEqualTo("dataset");
        assertThat(copy.maxThreadCount()).isEqualTo(4);
        assertThat(copy.tags()).containsExactly("nightly");
    }

    @Test
    void deserializesOptionsFromJson() throws JsonProcessingException {
        String json = """
                {
                  "name": "dataset",
                  "options": {
                    "solver": "fast",
                    "logLevel": "debug"
                  }
                }
                """;

        RunConfiguration configuration = mapper.readValue(json, RunConfiguration.class);

        assertThat(configuration.name()).isEqualTo("dataset");
        assertThat(configuration.options())
                .containsExactlyInAnyOrderEntriesOf(Map.of("solver", "fast", "logLevel", "debug"));
    }

    @Test
    void omitsNullOptionsFromJson() throws JsonProcessingException {
        String json = mapper.writeValueAsString(new RunConfiguration("dataset"));

        assertThat(json).doesNotContain("options");
    }

    @Test
    void serializesOptionsWhenPresent() throws JsonProcessingException {
        RunConfiguration configuration =
                new RunConfiguration(null, null, null, null, Map.of("solver", "fast"));

        String json = mapper.writeValueAsString(configuration);

        assertThat(json).contains("\"options\":{\"solver\":\"fast\"}");
    }

    @Test
    void overrideFillsNullTagsFromFallback() {
        RunConfiguration primary = new RunConfiguration("dataset", null, null, null, null);
        RunConfiguration fallback = new RunConfiguration(null, null, null, Set.of("nightly"), null);

        assertThat(primary.override(fallback).tags()).containsExactly("nightly");
    }

    @Test
    void overrideFillsEmptyTagsFromFallback() {
        RunConfiguration primary = new RunConfiguration("dataset", null, null, Set.of(), null);
        RunConfiguration fallback = new RunConfiguration(null, null, null, Set.of("nightly"), null);

        assertThat(primary.override(fallback).tags()).containsExactly("nightly");
    }

    @Test
    void overrideKeepsPrimaryTagsWhenPresent() {
        RunConfiguration primary = new RunConfiguration("dataset", null, null, Set.of("adhoc"), null);
        RunConfiguration fallback = new RunConfiguration(null, null, null, Set.of("nightly"), null);

        assertThat(primary.override(fallback).tags()).containsExactly("adhoc");
    }
}
