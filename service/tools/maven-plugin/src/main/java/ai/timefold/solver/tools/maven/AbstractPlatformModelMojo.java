package ai.timefold.solver.tools.maven;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipFile;

import ai.timefold.solver.tools.maven.client.PlatformIdentityInfo;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.crypto.SettingsDecrypter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class AbstractPlatformModelMojo extends AbstractMojo {

    private static final String DESCRIPTOR_FILE_NAME = "timefold-model-descriptor.json";

    public static final String PROP_DRY_RUN = "timefold.dryRun";

    protected static final String PROP_PLATFORM_URL = "timefold.platformUrl";

    protected static final String PROP_MODEL_KEY = "timefold.model.key";

    protected static final String PROP_MODEL_TENANTS = "timefold.model.tenants";

    protected static final String PROP_MODEL_SUBS = "timefold.model.handleSubscription";

    protected static final String PROP_SERVER_ID = "timefold.serverId";

    @Parameter(defaultValue = "${session}", readonly = true)
    protected MavenSession session;

    @Component
    private SettingsDecrypter settingsDecrypter;

    /**
     * Id of the {@code <server>} entry in the Maven settings that holds the personal access token, as an alternative
     * to exporting it as {@code TIMEFOLD_PAT}
     */
    @Parameter(property = PROP_SERVER_ID, required = false, defaultValue = AccessTokenProvider.DEFAULT_SERVER_ID)
    protected String serverId;

    /**
     * Built lazily, as it needs the settings of the session the mojo runs in; tests replace it with a double.
     */
    private AccessTokenProvider accessTokenProvider;

    /**
     * URL to the platform that model should be deployed to
     */
    @Parameter(defaultValue = "${project.build.directory}", required = false)
    protected String buildDirectory;

    /**
     * URL to the platform that model should be deployed to
     */
    @Parameter(property = PROP_PLATFORM_URL, required = true)
    protected String platformUrl;

    /**
     * Unique key used to register model with - used also when unregistering
     */
    @Parameter(property = PROP_MODEL_KEY, required = true)
    protected String key;

    /**
     * List of tenants this model should be registered for - used when type set to shared or private (in that case single tenant
     * should be set)
     */
    @Parameter(property = PROP_MODEL_TENANTS, required = false)
    private List<String> tenants;

    /**
     * Determines if the model registration should automatically subscribe/unsubscribe to the registered model
     */
    @Parameter(property = PROP_MODEL_SUBS, required = false, defaultValue = "false")
    protected boolean handleSubscription;

    protected ObjectMapper mapper = new ObjectMapper();

    protected HttpClient httpClient = HttpClient.newBuilder().version(Version.HTTP_2).followRedirects(Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10)).build();

    protected AccessTokenProvider getAccessTokenProvider() {
        if (accessTokenProvider == null) {
            accessTokenProvider = new AccessTokenProvider(session == null ? null : session.getSettings(),
                    settingsDecrypter, getConfiguredServerId(), getLog());
        }
        return accessTokenProvider;
    }

    protected void setAccessTokenProvider(AccessTokenProvider provider) {
        this.accessTokenProvider = provider;
    }

    protected PlatformIdentityInfo fetchPlatformIdentityInfo(boolean includeConfig) throws MojoExecutionException {
        var platformPAT = requireAccessToken();

        var requestBuilder = HttpRequest.newBuilder().GET();
        requestBuilder.header("Accept", "application/json");
        requestBuilder.header("Authorization", "Bearer " + platformPAT);
        requestBuilder.uri(URI.create(getPlatformUrl() + "/api/platform/v1/aboutme?includeConfig=" + includeConfig));

        var httpRequest = requestBuilder.build();
        try {
            var authResponse = httpClient.send(httpRequest, BodyHandlers.ofString());
            if (authResponse.statusCode() == 200) {
                return mapper.readValue(authResponse.body(), PlatformIdentityInfo.class);
            } else {
                if (getLog().isDebugEnabled()) {
                    getLog().debug(
                            "Platform authentication failure, status code %d, body %s".formatted(authResponse.statusCode(),
                                    authResponse.body()));
                }
                throw new MojoExecutionException(
                        "Platform authentication failed — please verify your PAT and tenant access, or contact support if the problem persists");
            }
        } catch (MojoExecutionException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MojoExecutionException("Interrupted while making platform info call", e);
        } catch (IOException e) {
            throw new MojoExecutionException("Unexpected error while making platform info call", e);
        }
    }

    /**
     * Resolves the personal access token, failing the build when none is configured. Without this the request goes out
     * with an empty bearer token and the platform answers with an authentication error, which points at the token
     * being wrong rather than at it never having been configured.
     * <p>
     * Deliberately called while a request is built rather than up front, so that the goals which send nothing on a
     * dry run still run without a token: {@code deploy} and {@code undeploy} only reach here once they have decided
     * to actually call the platform. {@code configure} reads the platform configuration even on a dry run, as it has
     * to write the registry and account id it would build with, so that goal needs a token either way.
     *
     * @throws MojoExecutionException when no token is configured, or when the configured one cannot be read
     */
    protected String requireAccessToken() throws MojoExecutionException {
        String accessToken = getAccessTokenProvider().getAccessToken();
        if (accessToken == null || accessToken.isBlank()) {
            throw new MojoExecutionException("""
                    Personal Access Token for Timefold Platform is required.
                    Either export it for this build:
                      export %s=<your token>
                    or store it, encrypted, in your Maven settings (~/.m2/settings.xml):
                      <server>
                        <id>%s</id>
                        <password>{encrypted token}</password>
                      </server>
                    Encrypt the token with 'mvn --encrypt-password', after creating a master password with \
                    'mvn --encrypt-master-password'; see %s
                    See https://docs.timefold.ai/timefold-solver/latest/deploying-to-platform/guide"""
                    .formatted(AccessTokenProvider.PAT_ENV_VARIABLE, getAccessTokenProvider().getServerId(),
                            AccessTokenProvider.ENCRYPTION_GUIDE_URL));
        }
        return accessToken;
    }

    /**
     * The raw configured value, which {@link AccessTokenProvider} normalizes; read it back from there rather than
     * here whenever it is reported, so that it names the entry that is actually looked up.
     */
    private String getConfiguredServerId() {
        return session == null ? serverId : getPropertyOrParameter(PROP_SERVER_ID, serverId);
    }

    protected void configureHttpRequest(Builder builder) throws MojoExecutionException {
        builder.timeout(Duration.ofSeconds(30));
        builder.header("Authorization", "Bearer " + requireAccessToken());
        builder.header("Content-Type", "application/octet-stream");
        builder.header("Accept", "application/json");
        var tenants = getTenants();
        if (tenants != null && !tenants.isEmpty()) {
            builder.header("X-TF-TENANT-ID", tenants.getFirst());
            getLog().debug("Tenant " + tenants.getFirst() + " is used as context of the request");
        }
    }

    /**
     * @throws MojoExecutionException when a mandatory parameter is not configured; a missing parameter is a build
     *         configuration problem, so it has to be reported as one rather than as an internal error
     */
    protected void validate() throws MojoExecutionException {
        if (platformUrl == null) {
            throw new MojoExecutionException("Platform Url is mandatory");
        }
        if (key == null) {
            throw new MojoExecutionException("Registration key is mandatory");
        }
    }

    protected void printErrorInfo(String responseBody) {
        if (responseBody != null && !responseBody.isBlank()) {
            getLog().error(responseBody);
        }
    }

    /**
     * Reads the platform error message from an error response body, so that the reason for the failure is part of the
     * reported error and not only of the build log. Falls back to the response body itself, as the platform does not
     * report every error as an {@code ErrorInfo}.
     */
    protected String readErrorMessage(String responseBody) {
        var message = readErrorField(responseBody, "message");
        if (message != null && !message.isBlank()) {
            return message;
        }
        return responseBody == null || responseBody.isBlank() ? "no error message reported by the platform" : responseBody;
    }

    protected String readErrorField(String responseBody, String fieldName) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        try {
            var field = mapper.readTree(responseBody).get(fieldName);
            return field == null || field.isNull() ? null : field.asText();
        } catch (IOException e) {
            getLog().debug("Unable to read error " + fieldName + " from response body " + responseBody, e);
            return null;
        }
    }

    /**
     * Resolves the configured platform URL, stripping any trailing slashes so it can be
     * safely concatenated with a path that starts with a slash (e.g. "/api/platform/v1/...").
     *
     * @throws MojoExecutionException when no platform URL is configured, which no goal can work without
     */
    protected String getPlatformUrl() throws MojoExecutionException {
        var url = getPropertyOrParameter(PROP_PLATFORM_URL, this.platformUrl);
        if (url != null) {
            url = url.trim();
            var end = url.length();
            while (end > 0 && url.charAt(end - 1) == '/') {
                end--;
            }
            url = url.substring(0, end);
        }
        if (url == null || url.isEmpty()) {
            throw new MojoExecutionException("Platform Url is mandatory");
        }
        return url;
    }

    protected ObjectNode readModelDescriptor(Path modelDescriptorArchivePath) throws IOException {
        var modelDescriptorPath = Paths.get(buildDirectory, "timefold", DESCRIPTOR_FILE_NAME);

        if (Files.exists(modelDescriptorPath)) {

            return (ObjectNode) mapper.readTree(Files.readAllBytes(modelDescriptorPath));
        } else {
            // extract model descriptor json from the archive
            if (modelDescriptorArchivePath == null || !Files.exists(modelDescriptorArchivePath)) {
                throw new IOException("Model descriptor archive not found: " + modelDescriptorArchivePath);
            }

            try (var zip = new ZipFile(modelDescriptorArchivePath.toFile())) {
                var entry = zip.getEntry(DESCRIPTOR_FILE_NAME);

                // if not found by exact name, search entries for a matching file name
                if (entry == null) {
                    var entries = zip.entries();
                    while (entries.hasMoreElements()) {
                        var e = entries.nextElement();
                        if (!e.isDirectory() && e.getName().endsWith(DESCRIPTOR_FILE_NAME)) {
                            entry = e;
                            break;
                        }
                    }
                }

                if (entry == null) {
                    throw new IOException(DESCRIPTOR_FILE_NAME + " not found in archive: " + modelDescriptorArchivePath);
                }

                try (var in = zip.getInputStream(entry)) {
                    return (ObjectNode) mapper.readTree(in);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected <T> T getPropertyOrParameter(String propertyName, T parameter) {
        var value = session.getUserProperties().getOrDefault(propertyName, parameter);

        if (value != null && parameter != null) {

            if (parameter instanceof Boolean) {
                value = Boolean.parseBoolean(value.toString());
                return (T) value;
            }
        }
        return (T) value;
    }

    public List<String> getTenants() {

        var stringTenants = session.getUserProperties().getProperty(PROP_MODEL_TENANTS);

        if (stringTenants != null && !stringTenants.isBlank()) {
            return Arrays.asList(stringTenants.split(","));
        }

        return tenants;
    }

}