package ai.timefold.solver.service.quarkus.deployment.rest.io;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import ai.timefold.solver.service.definition.api.dto.dataset.Configuration;
import ai.timefold.solver.service.definition.api.dto.dataset.ModelConfigOverrides;
import ai.timefold.solver.service.definition.api.dto.patch.ModelInputPatchRequest;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

class AbstractModelInputPatchRequestMessageBodyReadersTest {

    private final AbstractModelInputPatchRequestMessageBodyReaders<TestdataModelConfigOverrides> reader =
            new AbstractModelInputPatchRequestMessageBodyReaders<>() {
                @Override
                protected TypeReference<ModelInputPatchRequest<TestdataModelConfigOverrides>> typeRef() {
                    return new TypeReference<>() {
                    };
                }
            };

    AbstractModelInputPatchRequestMessageBodyReadersTest() {
        reader.mapper = new ObjectMapper();
    }

    @Test
    void readsOnlyTheModelInputPatchRequest() {
        assertThat(reader.isReadable(ModelInputPatchRequest.class, null, null, null)).isTrue();
        assertThat(reader.isReadable(Configuration.class, null, null, null)).isFalse();
    }

    @Test
    void readsTheOverridesAsTheClassOfTheModel() throws IOException {
        var body = """
                {"config": {"model": {"overrides": {"weight": 2}}}, "patch": []}""";

        var patchRequest = reader.readFrom(null, null, null, null, null,
                new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));

        assertThat(patchRequest.config().model().overrides())
                .isEqualTo(new TestdataModelConfigOverrides(2));
        assertThat(patchRequest.patch()).isEmpty();
    }

    private record TestdataModelConfigOverrides(Integer weight) implements ModelConfigOverrides {
    }
}
