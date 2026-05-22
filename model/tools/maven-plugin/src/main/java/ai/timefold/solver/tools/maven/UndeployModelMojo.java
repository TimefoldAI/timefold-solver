package ai.timefold.solver.tools.maven;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Goal that undeploy (unregisters model descriptor) model from platform
 */
@Mojo(name = "undeploy", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST)
public class UndeployModelMojo extends AbstractPlatformModelMojo {

    protected static final String PROP_MODEL_SKIP_UNDEPLOY = "timefold.model.undeploy.skip";

    /**
     * Determines if the model unregistration should be skipped
     */
    @Parameter(property = PROP_MODEL_SKIP_UNDEPLOY, required = false, defaultValue = "false")
    private boolean skip;

    /**
     * Determines if the model unregistration should be done as dry run
     */
    @Parameter(property = PROP_DRY_RUN, required = false, defaultValue = "false")
    private boolean dryRun;

    public void execute() throws MojoExecutionException {
        if (getPropertyOrParameter(PROP_MODEL_SKIP_UNDEPLOY, skip)) {
            getLog().info("Model undeployment skipped by configuration");
            return;
        }

        try {
            String platformUrl = getPropertyOrParameter(PROP_PLATFORM_URL, this.platformUrl);
            String key = getPropertyOrParameter(PROP_MODEL_KEY, this.key);
            Path modelDescriptorArchivePath = Paths.get(buildDirectory, "model-descriptor.zip");

            if (!Files.exists(modelDescriptorArchivePath)) {
                throw new IllegalStateException("Model descriptor not found in target folder");
            }
            ObjectNode modelDescriptor = readModelDescriptor(modelDescriptorArchivePath);
            getLog().info(String.format("Model %s (%s) is going to be undeployed from platform %s with registration key %s",
                    modelDescriptor.get("name").asText(), modelDescriptor.get("id").asText(), platformUrl, key));

            URI requestURI =
                    URI.create(platformUrl + "/api/platform/v1/models/" + key + "?handleSubscription="
                            + getPropertyOrParameter(PROP_MODEL_SUBS, handleSubscription));

            if (dryRun) {
                getLog().info("DRY_RUN: Would perform DELETE on " + requestURI);
            } else {

                Builder builder = HttpRequest.newBuilder().uri(requestURI).DELETE();

                configureHttpRequest(builder);

                HttpResponse<String> response = httpClient.send(builder.build(), BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 400) {

                    getLog().info(
                            String.format(
                                    "Model %s (%s) has been successfully undeployed from platform %s with registration key %s",
                                    modelDescriptor.get("name").asText(), modelDescriptor.get("id").asText(), platformUrl,
                                    key));
                    return;
                } else {
                    getLog().debug(response.body());
                    throw new IllegalStateException(
                            "Model undeploy failed with " + response.statusCode() + " status code");
                }
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error while undeploying model", e);
        }

    }
}
