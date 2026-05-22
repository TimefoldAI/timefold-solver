package ai.timefold.solver.tools.maven;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest.Builder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class AbstractPlatformModelMojo extends AbstractMojo {

    private AccessTokenProvider accessTokenProvider = new AccessTokenProvider();

    private static final String DESCRIPTOR_FILE_NAME = "timefold-model-descriptor.json";

    public static final String PROP_DRY_RUN = "timefold.dryRun";

    protected static final String PROP_PLATFORM_URL = "timefold.platformUrl";

    protected static final String PROP_MODEL_KEY = "timefold.model.key";

    protected static final String PROP_MODEL_TENANTS = "timefold.model.tenants";

    protected static final String PROP_MODEL_SUBS = "timefold.model.handleSubscription";

    @Parameter(defaultValue = "${session}", readonly = true)
    protected MavenSession session;

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

    protected void configureHttpRequest(Builder builder) {
        builder.timeout(Duration.ofSeconds(30));
        builder.header("Authorization", "Bearer " + accessTokenProvider.getAccessToken());
        builder.header("Content-Type", "application/octet-stream");
        builder.header("Accept", "application/json");
        List<String> tenants = getTenants();
        if (tenants != null && !tenants.isEmpty()) {
            builder.header("X-TF-TENANT-ID", tenants.getFirst());
            getLog().debug("Tenant " + tenants.getFirst() + " is used as context of the request");
        }
    }

    protected void validate() {
        Objects.requireNonNull(platformUrl, "Platform Url is mandatory");
        Objects.requireNonNull(key, "Registration key is mandatory");
    }

    protected ObjectNode readModelDescriptor(Path modelDescriptorArchivePath) throws IOException {
        Path modelDescriptorPath = Paths.get(buildDirectory, "timefold", DESCRIPTOR_FILE_NAME);

        if (Files.exists(modelDescriptorPath)) {

            return (ObjectNode) mapper.readTree(Files.readAllBytes(modelDescriptorPath));
        } else {
            // extract model descriptor json from the archive
            if (modelDescriptorArchivePath == null || !Files.exists(modelDescriptorArchivePath)) {
                throw new IOException("Model descriptor archive not found: " + modelDescriptorArchivePath);
            }

            try (ZipFile zip = new ZipFile(modelDescriptorArchivePath.toFile())) {
                ZipEntry entry = zip.getEntry(DESCRIPTOR_FILE_NAME);

                // if not found by exact name, search entries for a matching file name
                if (entry == null) {
                    Enumeration<? extends ZipEntry> entries = zip.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry e = entries.nextElement();
                        if (!e.isDirectory() && e.getName().endsWith(DESCRIPTOR_FILE_NAME)) {
                            entry = e;
                            break;
                        }
                    }
                }

                if (entry == null) {
                    throw new IOException(DESCRIPTOR_FILE_NAME + " not found in archive: " + modelDescriptorArchivePath);
                }

                try (InputStream in = zip.getInputStream(entry)) {
                    return (ObjectNode) mapper.readTree(in);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected <T> T getPropertyOrParameter(String propertyName, T parameter) {
        Object value = session.getUserProperties().getOrDefault(propertyName, parameter);

        if (value != null && parameter != null) {

            if (parameter instanceof Boolean) {
                value = Boolean.parseBoolean(value.toString());
                return (T) value;
            }
        }
        return (T) value;
    }

    public List<String> getTenants() {

        String stringTenants = session.getUserProperties().getProperty(PROP_MODEL_TENANTS);

        if (stringTenants != null && !stringTenants.isBlank()) {
            return Arrays.asList(stringTenants.split(","));
        }

        return tenants;
    }

}