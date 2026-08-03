package ai.timefold.solver.tools.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class AccessTokenProviderTest {

    @TempDir
    Path tempDir;

    @Test
    public void returnsEnvTokenWhenSet() {
        AccessTokenProvider provider = new AccessTokenProvider(tempDir.resolve("credentials")) {
            @Override
            protected String getEnvToken() {
                return "env-token";
            }
        };

        assertThat(provider.getAccessToken()).isEqualTo("env-token");
    }

    @Test
    public void fallsBackToCredentialsFileWhenEnvNotSet() throws IOException {
        Path credentialsFile = tempDir.resolve("credentials");
        Files.writeString(credentialsFile, "pat=file-token\n");

        AccessTokenProvider provider = new AccessTokenProvider(credentialsFile) {
            @Override
            protected String getEnvToken() {
                return null;
            }
        };

        assertThat(provider.getAccessToken()).isEqualTo("file-token");
    }

    @Test
    public void returnsNullWhenNeitherEnvNorFileIsSet() {
        AccessTokenProvider provider = new AccessTokenProvider(tempDir.resolve("credentials")) {
            @Override
            protected String getEnvToken() {
                return null;
            }
        };

        assertThat(provider.getAccessToken()).isNull();
    }
}
