package ai.timefold.solver.tools.maven;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import ai.timefold.solver.tools.maven.http.UploadProgressReporter;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.fasterxml.jackson.databind.JsonNode;
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

    /**
     * Sends the request and, while waiting for the response, lets the reporter log progress from this thread.
     * <p>
     * {@link HttpClient#sendAsync(HttpRequest, BodyHandler)} is used rather than
     * {@link HttpClient#send(HttpRequest, BodyHandler)} so that this thread stays free to log while the request body is
     * written by the client's own threads. The exception unwrapping below restores the exception types that the
     * blocking send would have thrown, so callers and their error messages do not have to change.
     */
    protected <T> HttpResponse<T> sendWithProgress(HttpRequest request, BodyHandler<T> responseBodyHandler,
            UploadProgressReporter reporter) throws IOException, InterruptedException {
        CompletableFuture<HttpResponse<T>> future = httpClient.sendAsync(request, responseBodyHandler);
        long heartbeatMillis = Math.max(1L, reporter.getHeartbeatInterval().toMillis());
        try {
            while (true) {
                try {
                    HttpResponse<T> response = future.get(heartbeatMillis, TimeUnit.MILLISECONDS);
                    reporter.finished();
                    return response;
                } catch (TimeoutException timeoutException) {
                    // not an error, the request is simply still in flight
                    reporter.heartbeat();
                }
            }
        } catch (ExecutionException executionException) {
            reporter.finished();
            Throwable cause = executionException.getCause();
            // HttpTimeoutException, ConnectException, SSLException, ... are all IOException subtypes and are rethrown
            // as is, exactly as httpClient.send() would have thrown them
            if (cause instanceof IOException ioException) {
                throw ioException;
            } else if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            } else if (cause instanceof Error error) {
                throw error;
            }
            throw new IOException(cause == null ? executionException.getMessage() : cause.getMessage(),
                    cause == null ? executionException : cause);
        } catch (InterruptedException interruptedException) {
            future.cancel(true);
            throw interruptedException;
        }
    }

    protected void validate() {
        Objects.requireNonNull(platformUrl, "Platform Url is mandatory");
        Objects.requireNonNull(key, "Registration key is mandatory");
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
        String message = readErrorField(responseBody, "message");
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
            JsonNode field = mapper.readTree(responseBody).get(fieldName);
            return field == null || field.isNull() ? null : field.asText();
        } catch (IOException e) {
            getLog().debug("Unable to read error " + fieldName + " from response body " + responseBody, e);
            return null;
        }
    }

    /**
     * Resolves the configured platform URL, stripping any trailing slashes so it can be
     * safely concatenated with a path that starts with a slash (e.g. "/api/platform/v1/...").
     */
    protected String getPlatformUrl() {
        String url = getPropertyOrParameter(PROP_PLATFORM_URL, this.platformUrl);
        if (url != null) {
            url = url.trim();
            int end = url.length();
            while (end > 0 && url.charAt(end - 1) == '/') {
                end--;
            }
            url = url.substring(0, end);
        }
        if (url == null || url.isEmpty()) {
            throw new IllegalStateException("Platform Url is mandatory");
        }
        return url;
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