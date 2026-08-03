package ai.timefold.solver.tools.maven;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class AccessTokenProvider {

    private static final Path DEFAULT_CREDENTIALS_FILE = Path.of(System.getProperty("user.home"), ".timefold", "credentials");

    private final Path credentialsFile;

    public AccessTokenProvider() {
        this(DEFAULT_CREDENTIALS_FILE);
    }

    AccessTokenProvider(Path credentialsFile) {
        this.credentialsFile = credentialsFile;
    }

    public String getAccessToken() {
        String envToken = getEnvToken();
        if (envToken != null && !envToken.isBlank()) {
            return envToken;
        }
        return readFromCredentialsFile();
    }

    protected String getEnvToken() {
        return System.getenv("TIMEFOLD_PAT");
    }

    private String readFromCredentialsFile() {
        if (!Files.isRegularFile(credentialsFile)) {
            return null;
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(credentialsFile)) {
            properties.load(input);
        } catch (IOException e) {
            return null;
        }
        return properties.getProperty("pat");
    }
}
