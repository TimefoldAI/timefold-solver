package ai.timefold.solver.service.maps.service.client.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ServiceAccountTokenHeadersFactoryTest {

    @TempDir
    Path directory;

    @Test
    void theServiceAccountTokenIsSentWithTheCall() throws IOException {
        Path file = directory.resolve("token");
        Files.writeString(file, "a.b.c");

        MultivaluedMap<String, String> headers = update(file.toString());

        assertThat(headers.getFirst("Authorization")).isEqualTo("Bearer a.b.c");
    }

    @Test
    void aModelWithoutAMountedTokenCallsTheMapServiceWithoutOne() {
        MultivaluedMap<String, String> headers = update(directory.resolve("not-mounted").toString());

        assertThat(headers).isEmpty();
    }

    private static MultivaluedMap<String, String> update(String tokenFile) {
        MultivaluedMap<String, String> outgoing = new MultivaluedHashMap<>();
        MultivaluedMap<String, String> headers =
                new ServiceAccountTokenHeadersFactory(tokenFile).update(new MultivaluedHashMap<>(), outgoing);
        // the outgoing headers are read-only, the factory hands its header back instead
        assertThat(outgoing).isEmpty();
        return headers;
    }
}
