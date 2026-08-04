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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Parent;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import com.fasterxml.jackson.databind.ObjectMapper;

@Mojo(name = "configure", defaultPhase = LifecyclePhase.INITIALIZE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class ConfigureMojo extends AbstractPlatformModelMojo {

    private HttpClient httpClient = HttpClient.newBuilder().followRedirects(Redirect.ALWAYS).build();
    private ObjectMapper mapper = new ObjectMapper();
    private AccessTokenProvider accessTokenProvider = new AccessTokenProvider();

    protected static final String PROP_ACCOUNT_ID = "timefold.accountId";

    protected static final String PROP_MODEL_NATIVE_SUPPORTED = "timefold.model.nativeSupported";

    protected static final String PROP_MODEL_CONFIG_SKIP = "timefold.model.configuration.skip";

    /**
     * Group id of the Enterprise Edition artifacts, pulled in by the {@code enterprise} profile of
     * {@code timefold-solver-service-parent}.
     */
    private static final String ENTERPRISE_GROUP_ID = "ai.timefold.solver.enterprise";

    private static final String PARENT_GROUP_ID = "ai.timefold.solver";

    private static final String PARENT_ARTIFACT_ID = "timefold-solver-service-parent";

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
        boolean deployRequested = shouldExecute();
        // Deliberately checked before the configuration skip, so that skipping the platform configuration
        // does not silently skip the Enterprise Edition check as well.
        if (deployRequested) {
            validateEnterpriseBuild();
        }
        if (getPropertyOrParameter(PROP_MODEL_CONFIG_SKIP, skip)) {
            getLog().info("Timefold Platform configuration skipped");
            return;
        }
        if (deployRequested) {
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
                        project.getProperties().getProperty("timefold.model.jvm-image-arguments", ""));
                timefoldBuildProperties.setProperty("quarkus.jib.base-jvm-image",
                        project.getProperties().getProperty("timefold.model.base-jvm-image",
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

    /**
     * Timefold Platform only accepts models that inherit from {@code timefold-solver-service-parent} and that are built
     * with the Enterprise Edition. Such a model builds and deploys successfully, but fails later, when it actually runs,
     * with errors that do not point at the missing profile.
     */
    protected void validateEnterpriseBuild() throws MojoFailureException {
        if (!inheritsFromServiceParent()) {
            throw new MojoFailureException("""
                    This model does not inherit from %s:%s, which Timefold Platform requires; without it the model \
                    descriptor and the container image are not built the way the platform expects.
                    Declare it as the parent of your model:
                      <parent>
                        <groupId>%s</groupId>
                        <artifactId>%s</artifactId>
                        <version>...</version>
                      </parent>
                    See https://docs.timefold.ai/timefold-solver/latest/deploying-to-platform/guide"""
                    .formatted(PARENT_GROUP_ID, PARENT_ARTIFACT_ID, PARENT_GROUP_ID, PARENT_ARTIFACT_ID));
        }

        if (!hasEnterpriseArtifacts()) {
            throw new MojoFailureException("""
                    This model was built with the Community Edition of Timefold Solver, but Timefold Platform only accepts \
                    models built with the Enterprise Edition. No %s artifact is on the classpath, so the deployed model \
                    would fail at runtime.
                    Activate the 'enterprise' Maven profile:
                      mvn clean package -Denterprise=true timefold:deploy
                    or add '-Penterprise' to your project's .mvn/maven.config.
                    See https://docs.timefold.ai/timefold-solver/latest/deploying-to-platform/guide#_enterprise_edition"""
                    .formatted(ENTERPRISE_GROUP_ID));
        }
    }

    private boolean inheritsFromServiceParent() {
        for (MavenProject ancestor = project.getParent(); ancestor != null; ancestor = ancestor.getParent()) {
            if (PARENT_GROUP_ID.equals(ancestor.getGroupId()) && PARENT_ARTIFACT_ID.equals(ancestor.getArtifactId())) {
                return true;
            }
        }
        // getParent() is not populated for every parent resolved from a repository, so also check the declared parent.
        Parent declaredParent = project.getModel().getParent();
        return declaredParent != null && PARENT_GROUP_ID.equals(declaredParent.getGroupId())
                && PARENT_ARTIFACT_ID.equals(declaredParent.getArtifactId());
    }

    private boolean hasEnterpriseArtifacts() {
        return project.getArtifacts().stream()
                .map(Artifact::getGroupId)
                .anyMatch(groupId -> ENTERPRISE_GROUP_ID.equals(groupId)
                        || groupId.startsWith(ENTERPRISE_GROUP_ID + "."));
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
        String platformUrl = getPlatformUrl();
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
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
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

    protected MavenProject getProject() {
        return project;
    }
}
