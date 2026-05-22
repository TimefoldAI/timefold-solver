package ai.timefold.solver.tools.maven;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import ai.timefold.solver.tools.maven.client.PlatformIdentityInfo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.fasterxml.jackson.databind.ObjectMapper;

@Mojo(name = "configure", defaultPhase = LifecyclePhase.INITIALIZE)
public class ConfigureMojo extends AbstractPlatformModelMojo {

    private HttpClient httpClient = HttpClient.newBuilder().followRedirects(Redirect.ALWAYS).build();
    private ObjectMapper mapper = new ObjectMapper();
    private AccessTokenProvider accessTokenProvider = new AccessTokenProvider();

    protected static final String PROP_ACCOUNT_ID = "timefold.accountId";

    protected static final String PROP_MODEL_NATIVE_SUPPORTED = "timefold.model.nativeSupported";

    protected static final String PROP_MODEL_CONFIG_SKIP = "timefold.model.configuration.skip";

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * Account id that model is associated with
     */
    @Parameter(property = PROP_ACCOUNT_ID, required = false)
    protected String accountId;

    /**
     * Determines if the native build of the model is supported and by that should be defined in model descriptor
     * For local builds this should be set to false to allow use jvm image instead
     */
    @Parameter(property = PROP_MODEL_NATIVE_SUPPORTED, required = false, defaultValue = "false")
    private boolean nativeSupported;

    /**
     * Determines if the platform configuration should be skipped
     */
    @Parameter(property = PROP_MODEL_CONFIG_SKIP, required = false, defaultValue = "false")
    private boolean skip;

    /**
     * Determines if the platform configuration should be done as dry run
     */
    @Parameter(property = PROP_DRY_RUN, required = false, defaultValue = "false")
    private boolean dryRun;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (getPropertyOrParameter(PROP_MODEL_CONFIG_SKIP, skip)) {
            getLog().info("Timefold Platform configuration skipped");
            return;
        }
        if (shouldExecute()) {
            try {
                PlatformIdentityInfo info = fetchPlatformConfiguration();

                if (info == null || !info.hasPushAccessRights()) {
                    throw new RuntimeException("No access to deploy model on Timefold Platform");
                }
                String accountId = getPropertyOrParameter(PROP_ACCOUNT_ID, this.accountId);
                if (accountId == null && info.accountIds().size() == 1) {
                    accountId = info.accountIds().iterator().next();
                }

                if (accountId != null && !info.hasAccessToAccountId(accountId)) {
                    throw new RuntimeException(
                            "No access to configured account id " + accountId + " or account not configured");
                }

                Path path = Paths.get("target", "generated-resources", "timefold-build.properties");
                File timefoldBuildPropertiesFile = path.toFile();

                Files.createDirectories(path.getParent());

                Properties timefoldBuildProperties = new Properties();

                String registry = info.config().containerRegistry();

                // configure quarkus container properties
                timefoldBuildProperties.setProperty("quarkus.profile", "container");
                timefoldBuildProperties.setProperty("quarkus.container-image.build", "true");
                timefoldBuildProperties.setProperty("quarkus.container-image.registry", registry);
                timefoldBuildProperties.setProperty("quarkus.container-image.group", accountId);

                // configure container image and arguments based on model parent pom settings
                timefoldBuildProperties.setProperty("quarkus.jib.jvm-additional-arguments",
                        project.getProperties().getProperty("ai.timefold.model.jvm-image-arguments", ""));
                timefoldBuildProperties.setProperty("quarkus.jib.base-jvm-image",
                        project.getProperties().getProperty("ai.timefold.model.base-jvm-image",
                                "must-be-set-from-parent-pom"));

                if (!getPropertyOrParameter(PROP_DRY_RUN, dryRun)) {
                    // for dry run don't include image push and multi architecture images
                    timefoldBuildProperties.setProperty("quarkus.container-image.push", "true");
                    timefoldBuildProperties.setProperty("quarkus.jib.platforms", "linux/amd64,linux/arm64/v8");

                    // configure container registry credentials as system properties to not write them to any files
                    System.setProperty("quarkus.container-image.username", "token");
                    System.setProperty("quarkus.container-image.password", accessTokenProvider.getAccessToken());
                }
                if (!getPropertyOrParameter(PROP_MODEL_NATIVE_SUPPORTED, nativeSupported)) {
                    // allow to use jvm image for native use cases
                    timefoldBuildProperties.setProperty("image.native-suffix", "");
                }

                try (FileOutputStream output = new FileOutputStream(timefoldBuildPropertiesFile)) {
                    timefoldBuildProperties.store(output, "Timefold Platform configuration");
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to store build properties", e);
                }

                getLog().info("Configured Timefold Platform integration");
            } catch (IOException e) {
                throw new MojoExecutionException(e);
            }
        }
    }

    private PlatformIdentityInfo fetchPlatformConfiguration() {
        String platformPAT = accessTokenProvider.getAccessToken();

        if (platformPAT == null) {
            throw new RuntimeException(
                    "Personal Access Token for Timefold Platform is required. Set this via TIMEFOLD_PAT environment variable");
        }

        Builder requestBuilder = HttpRequest.newBuilder().GET();
        requestBuilder.header("Accept", "application/json");

        requestBuilder.header("Authorization", "Bearer " + platformPAT);
        String platformUrl = getPropertyOrParameter(PROP_PLATFORM_URL, this.platformUrl);
        requestBuilder.uri(URI.create(platformUrl + "/api/platform/v1/aboutme?includeConfig=true"));

        HttpRequest httpRequest = requestBuilder.build();
        try {
            HttpResponse<String> authResponse = httpClient.send(httpRequest, BodyHandlers.ofString());
            if (authResponse.statusCode() == 200) {
                String payload = authResponse.body();

                return mapper.readValue(payload, PlatformIdentityInfo.class);
            } else {
                getLog().debug(authResponse.body());
                throw new IllegalStateException(
                        "Platform authentication failed with " + authResponse.statusCode() + " status code");
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error while making platform info call", e);
        }
    }

    /*
     * Executes only when timefold:deploy goal is requested
     */
    protected boolean shouldExecute() {

        List<String> goals = session.getRequest().getGoals();
        return goals.contains("timefold:deploy");
    }

    protected void setAccessTokenProvider(AccessTokenProvider provider) {
        this.accessTokenProvider = provider;
    }
}
