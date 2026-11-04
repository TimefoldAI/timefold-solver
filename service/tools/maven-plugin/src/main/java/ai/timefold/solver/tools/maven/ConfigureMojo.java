package ai.timefold.solver.tools.maven;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

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

@Mojo(name = "configure", defaultPhase = LifecyclePhase.INITIALIZE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class ConfigureMojo extends AbstractPlatformModelMojo {

    protected static final String PROP_NAMESPACE = "timefold.namespace";

    protected static final String PROP_MODEL_NATIVE_SUPPORTED = "timefold.model.nativeSupported";

    protected static final String PROP_MODEL_CONFIG_SKIP = "timefold.model.configuration.skip";

    /**
     * Name of the generated properties file,
     * written into {@code <module>/target/generated-resources}.
     */
    static final String BUILD_PROPERTIES_FILE_NAME = "timefold-build.properties";

    /**
     * System property through which this goal hands the absolute location of the generated file
     * to the Quarkus extension that reads it.
     * The goal and the Quarkus augmentation run in the same JVM,
     * and the augmentation has no way of knowing the module's build directory on its own,
     * as the configuration is read before any build step runs.
     * <p>
     * Must stay in sync with
     * {@code ai.timefold.solver.service.quarkus.deployment.config.TimefoldBuildConfigOverrides#BUILD_PROPERTIES_LOCATION};
     * the two modules do not share code, so the literal is deliberately duplicated and asserted on both sides.
     */
    static final String BUILD_PROPERTIES_LOCATION = "timefold.build.properties.location";

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
     * Namespace that model is associated with
     */
    @Parameter(property = PROP_NAMESPACE, required = false)
    protected String namespace;

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
        // A previously built module in the same reactor may have left its own location behind;
        // only a successful write below may set it again.
        System.clearProperty(BUILD_PROPERTIES_LOCATION);

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
                PlatformIdentityInfo info = fetchPlatformIdentityInfo(true);

                if (info == null || !info.hasPushAccessRights()) {
                    throw new MojoFailureException("No access to deploy model on Timefold Platform");
                }
                var resolvedNamespace = resolveNamespace(info);

                if (!info.hasAccessToNamespace(resolvedNamespace)) {
                    // Only a namespace configured explicitly can get here; one derived from the token is always allowed.
                    throw new MojoFailureException(describeMissingNamespaceAccess(info, resolvedNamespace));
                }

                Path timefoldBuildPropertiesPath = resolveBuildPropertiesPath();
                Files.createDirectories(timefoldBuildPropertiesPath.getParent());

                Properties timefoldBuildProperties = new Properties();

                String registry = info.config().containerRegistry();

                // configure quarkus container properties
                timefoldBuildProperties.setProperty("quarkus.profile", "container");
                timefoldBuildProperties.setProperty("quarkus.container-image.build", "true");
                timefoldBuildProperties.setProperty("quarkus.container-image.registry", registry);
                timefoldBuildProperties.setProperty("quarkus.container-image.group", resolvedNamespace);

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
                    System.setProperty("quarkus.container-image.password", requireAccessToken());
                }
                if (!getPropertyOrParameter(PROP_MODEL_NATIVE_SUPPORTED, nativeSupported)) {
                    // allow to use jvm image for native use cases
                    timefoldBuildProperties.setProperty("image.native-suffix", "");
                }

                try (FileOutputStream output = new FileOutputStream(timefoldBuildPropertiesPath.toFile())) {
                    timefoldBuildProperties.store(output, "Timefold Platform configuration");
                } catch (IOException e) {
                    throw new MojoExecutionException(
                            "Unable to store the build properties in (%s).".formatted(timefoldBuildPropertiesPath), e);
                }
                System.setProperty(BUILD_PROPERTIES_LOCATION, timefoldBuildPropertiesPath.toString());

                getLog().info("Configured Timefold Platform integration; build properties written to "
                        + timefoldBuildPropertiesPath);
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to configure the Timefold Platform integration", e);
            }
        }
    }

    /**
     * Resolves the namespace the model is deployed under, which becomes the group of the container image. It is either
     * configured explicitly, or, when the personal access token is associated with exactly one namespace, that namespace.
     *
     * @throws MojoFailureException when the namespace is neither configured nor unambiguously derivable from the
     *         personal access token; without it the container image cannot be named, so the build must not continue.
     */
    protected String resolveNamespace(PlatformIdentityInfo info) throws MojoFailureException {
        String configuredNamespace = getPropertyOrParameter(PROP_NAMESPACE, this.namespace);
        if (configuredNamespace != null && !configuredNamespace.isBlank()) {
            return configuredNamespace.trim();
        }

        Set<String> namespaces = info.namespaces();
        if (namespaces.size() == 1) {
            return namespaces.iterator().next();
        }

        if (namespaces.isEmpty()) {
            throw new MojoFailureException("""
                    Unable to resolve the Timefold Platform namespace: the personal access token is not associated with \
                    any namespace, so the container image of this model cannot be built.
                    Use a personal access token that is associated with a namespace allowed to deploy models.
                    See https://docs.timefold.ai/timefold-solver/latest/deploying-to-platform/guide""");
        }
        throw new MojoFailureException("""
                Unable to resolve the Timefold Platform namespace: the personal access token is associated with %d \
                namespaces (%s), so the namespace to deploy this model to has to be configured explicitly.
                Either pass it on the command line:
                  mvn clean package -D%s=<namespace> timefold:deploy
                or declare it in the plugin configuration:
                  <configuration>
                    <namespace>...</namespace>
                  </configuration>
                See https://docs.timefold.ai/timefold-solver/latest/deploying-to-platform/guide"""
                .formatted(namespaces.size(), namespaces.stream().sorted().collect(Collectors.joining(", ")),
                        PROP_NAMESPACE));
    }

    /**
     * Explains why the configured namespace cannot be deployed to, telling a namespace the token does not grant apart
     * from a token that grants no namespace at all, as those need different fixes.
     */
    private static String describeMissingNamespaceAccess(PlatformIdentityInfo info, String configuredNamespace) {
        if (info.namespaces().isEmpty()) {
            return "The personal access token is not associated with any namespace, so this model cannot be deployed "
                    + "to the configured namespace " + configuredNamespace;
        }
        return "The personal access token is not associated with the configured namespace %s, but with %s"
                .formatted(configuredNamespace, info.namespaces().stream().sorted().collect(Collectors.joining(", ")));
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

    /*
     * Executes only when timefold:deploy goal is requested
     */
    protected boolean shouldExecute() {
        List<String> goals = session.getRequest().getGoals();
        return goals.contains("timefold:deploy");
    }

    /**
     * Resolves the generated properties file inside the build directory of the module being built,
     * rather than relative to the working directory.
     * In a multi-module build, or whenever Maven is started from a parent directory,
     * the working directory is the directory Maven was launched in and not the module's own directory,
     * so a relative path puts the file in the wrong module's {@code target}.
     *
     * @return the absolute location of the file to generate
     */
    private Path resolveBuildPropertiesPath() {
        return Paths.get(buildDirectory, "generated-resources", BUILD_PROPERTIES_FILE_NAME)
                .toAbsolutePath()
                .normalize();
    }

    protected MavenProject getProject() {
        return project;
    }
}
