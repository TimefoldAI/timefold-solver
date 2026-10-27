package ai.timefold.solver.service.quarkus.deployment.descriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import ai.timefold.solver.service.definition.api.usage.UniqueResourceId;
import ai.timefold.solver.service.definition.internal.descriptor.usage.UniqueResource;

import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

class UniqueResourceIdPathResolverTest {

    @Test
    void multipleResourceTypes() throws IOException {
        var index = Index.of(InputWithMultipleResources.class, Vehicle.class, Visit.class, Depot.class);

        assertThat(resolve(index, InputWithMultipleResources.class)).containsExactly(
                new UniqueResource("depot", "$.depotsByName.*.code"),
                new UniqueResource("vehicle", "$.vehicles[*].id"),
                new UniqueResource("visit", "$.unassignedVisits[*].visitId"),
                new UniqueResource("visit", "$.vehicles[*].visits[*].visitId"));
    }

    @Test
    void noResourceId() throws IOException {
        var index = Index.of(NoResourceIdInput.class);

        assertThat(resolve(index, NoResourceIdInput.class)).isEmpty();
    }

    @Test
    void blankResourceType() throws IOException {
        var index = Index.of(BlankTypeInput.class);

        assertThatIllegalStateException()
                .isThrownBy(() -> resolve(index, BlankTypeInput.class))
                .withMessageContaining("id")
                .withMessageContaining("blank type");
    }

    private static List<UniqueResource> resolve(IndexView index, Class<?> modelInput) {
        return ResourceIdPathResolver.resolve(index, index.getClassByName(modelInput));
    }

    static class InputWithMultipleResources {
        List<Vehicle> vehicles;
        Map<String, Depot> depotsByName;
        Visit[] unassignedVisits;
        @JsonIgnore
        List<Vehicle> ignoredVehicles;
    }

    static class Vehicle {
        @UniqueResourceId(type = "vehicle")
        String id;
        List<Visit> visits;
    }

    static class Visit {
        @JsonProperty("visitId")
        @UniqueResourceId(type = "visit")
        String id;
    }

    static class Depot {
        @UniqueResourceId(type = "depot")
        String code;
    }

    static class NoResourceIdInput {
        String id;
    }

    static class BlankTypeInput {
        @UniqueResourceId(type = " ")
        String id;
    }
}
