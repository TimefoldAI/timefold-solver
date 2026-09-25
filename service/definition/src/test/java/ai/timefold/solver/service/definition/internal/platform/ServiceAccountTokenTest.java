package ai.timefold.solver.service.definition.internal.platform;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ServiceAccountTokenTest {

    @TempDir
    Path directory;

    @Test
    void theTokenIsSentAsABearerCredential() throws IOException {
        Path file = tokenFile("a.b.c");

        assertThat(new ServiceAccountToken(file.toString()).authorization()).hasValue("Bearer a.b.c");
    }

    /**
     * The file ends with a newline in some clusters, which is not part of the token.
     */
    @Test
    void theTokenIsReadWithoutTheWhitespaceAroundIt() throws IOException {
        Path file = tokenFile("\na.b.c\n");

        assertThat(new ServiceAccountToken(file.toString()).authorization()).hasValue("Bearer a.b.c");
    }

    /**
     * Anything that does not run in a cluster has no token mounted, which is not an error: the request is sent
     * without one and the service on the other end decides what to make of that.
     */
    @Test
    void thereIsNoTokenWhenNoneIsMounted() {
        ServiceAccountToken token = new ServiceAccountToken(directory.resolve("not-mounted").toString());

        assertThat(token.authorization()).isEmpty();
    }

    @Test
    void thereIsNoTokenWhenTheFileIsEmpty() throws IOException {
        Path file = tokenFile("  \n");

        assertThat(new ServiceAccountToken(file.toString()).authorization()).isEmpty();
    }

    /**
     * The kubelet rewrites the file in place when it rotates the token, so holding on to the first one that was read
     * would mean sending an expired credential for the rest of the life of the pod.
     */
    @Test
    void aRotatedTokenIsPickedUp() throws IOException {
        Path file = tokenFile("first");
        ServiceAccountToken token = new ServiceAccountToken(file, Duration.ZERO);
        assertThat(token.authorization()).hasValue("Bearer first");

        Files.writeString(file, "second");

        assertThat(token.authorization()).hasValue("Bearer second");
    }

    /**
     * Reading the file on every request would be a syscall per storage operation, and the token is good for hours.
     */
    @Test
    void theTokenIsNotReadAgainForEveryRequest() throws IOException {
        Path file = tokenFile("first");
        ServiceAccountToken token = new ServiceAccountToken(file, Duration.ofHours(1));
        assertThat(token.authorization()).hasValue("Bearer first");

        Files.writeString(file, "second");

        assertThat(token.authorization()).hasValue("Bearer first");
    }

    /**
     * A pod that is not mounted a token may still be given one later on, so the absence of the file is not final
     * either.
     */
    @Test
    void aTokenThatAppearsLaterIsPickedUp() throws IOException {
        Path file = directory.resolve("token");
        ServiceAccountToken token = new ServiceAccountToken(file, Duration.ZERO);
        assertThat(token.authorization()).isEmpty();

        Files.writeString(file, "mounted");

        assertThat(token.authorization()).hasValue("Bearer mounted");
    }

    private Path tokenFile(String content) throws IOException {
        Path file = directory.resolve("token");
        Files.writeString(file, content);
        return file;
    }
}
