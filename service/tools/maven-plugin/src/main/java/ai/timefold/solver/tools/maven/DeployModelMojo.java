package ai.timefold.solver.tools.maven;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Goal that deploy (registers model descriptor) model to platform
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class DeployModelMojo extends AbstractPlatformModelMojo {

    private static final String PRIVATE_MODEL_TYPE = "Private";
    private static final String SHARED_MODEL_TYPE = "Shared";

    /**
     * Error codes the platform reports on a conflict (HTTP 409). Only a conflict on the registration key itself or on
     * the version registered under it can be resolved by updating that registration, any other conflict, including one
     * the platform does not identify, fails the deployment as updating this registration key does not resolve it.
     */
    private static final String ERROR_CODE_REGISTRATION_KEY_ALREADY_EXISTS = "TFP-14001";
    private static final String ERROR_CODE_MODEL_VERSION_ALREADY_EXISTS = "TFP-14005";
    private static final String ERROR_CODE_UNKNOWN = "TFP-99999";

    private static final List<String> OVERWRITABLE_CONFLICT_ERROR_CODES =
            List.of(ERROR_CODE_REGISTRATION_KEY_ALREADY_EXISTS, ERROR_CODE_MODEL_VERSION_ALREADY_EXISTS);

    protected static final String PROP_MODEL_TYPE = "timefold.model.type";

    protected static final String PROP_MODEL_OVERWRITE = "timefold.model.overwrite";

    protected static final String PROP_MODEL_SKIP_DEPLOY = "timefold.model.deploy.skip";

    protected static final String PROP_MODEL_DEPLOY_DESCRIPTOR_ONLY = "timefold.model.deploy.descriptorOnly";

    /**
     * Type of model registration - public, shared, private
     */
    @Parameter(property = PROP_MODEL_TYPE, required = false)
    private String type;

    /**
     * Determines if the model registration should overwrite already registered with the same registration key
     */
    @Parameter(property = PROP_MODEL_OVERWRITE, required = false, defaultValue = "false")
    private boolean overwrite;

    /**
     * Determines if the model registration should be skipped
     */
    @Parameter(property = PROP_MODEL_SKIP_DEPLOY, required = false, defaultValue = "false")
    private boolean skip;

    /**
     * Determines if the model registration should be proceed even when package of the application was not requested
     */
    @Parameter(property = PROP_MODEL_DEPLOY_DESCRIPTOR_ONLY, required = false, defaultValue = "false")
    private boolean descriptorOnly;

    /**
     * Determines if the model registration should be done as dry run
     */
    @Parameter(property = PROP_DRY_RUN, required = false, defaultValue = "false")
    private boolean dryRun;

    public void execute() throws MojoExecutionException {
        if (getPropertyOrParameter(PROP_MODEL_SKIP_DEPLOY, skip)) {
            getLog().info("Model deployment skipped by configuration");
            return;
        }

        // ensure that package goal was used or explicitly allow to deploy without build (and by that push of container image)
        List<String> goals = session.getRequest().getGoals();
        if (!goals.contains("package") && !getPropertyOrParameter(PROP_MODEL_DEPLOY_DESCRIPTOR_ONLY, descriptorOnly)) {
            throw new IllegalStateException(
                    "'package' goal was not requested, deploy of timefold model might not be complete, make sure to use 'clean package timefold:deploy' or set '-Dtimefold.model.deploy.descriptorOnly=true'");
        }

        Path modelDescriptorArchivePath = Paths.get(buildDirectory, "model-descriptor.zip");

        if (!Files.exists(modelDescriptorArchivePath)) {
            throw new IllegalStateException("Model descriptor not found in target folder");
        }

        try {
            String platformUrl = getPlatformUrl();
            String modelType = getPropertyOrParameter(PRIVATE_MODEL_TYPE, type);
            List<String> tenants = getTenants();
            if (modelType == null) {
                if (tenants != null && tenants.size() > 1) {
                    modelType = SHARED_MODEL_TYPE;
                } else {
                    modelType = PRIVATE_MODEL_TYPE;
                }
                getLog().debug(
                        "Type is not explicitly specified so it was computed based on tenants and is set to " + modelType);
            }
            validate(modelType);

            String key = getPropertyOrParameter(PROP_MODEL_KEY, this.key);
            ObjectNode modelDescriptor = readModelDescriptor(modelDescriptorArchivePath);
            getLog().info(String.format("Model %s (%s) is going to be deployed into platform %s with registration key %s",
                    modelDescriptor.get("name").asText(), modelDescriptor.get("id").asText(), platformUrl, key));

            StringBuilder queryString = new StringBuilder();
            queryString.append("type=").append(modelType);
            if (modelType.equalsIgnoreCase(SHARED_MODEL_TYPE)) {
                tenants.forEach(tenant -> queryString.append("&restrictedTo=").append(tenant));
            }
            queryString.append("&registrationKey=").append(key);
            queryString.append("&handleSubscription=").append(getPropertyOrParameter(PROP_MODEL_SUBS, handleSubscription));
            URI requestURI = URI.create(platformUrl + "/api/platform/v1/models?" + queryString.toString());

            if (getPropertyOrParameter(PROP_DRY_RUN, dryRun)) {
                getLog().info("DRY_RUN: Would perform POST on " + requestURI);
            } else {

                Builder builder =
                        HttpRequest.newBuilder().uri(requestURI).POST(BodyPublishers.ofFile(modelDescriptorArchivePath));

                configureHttpRequest(builder);

                HttpResponse<String> response = httpClient.send(builder.build(), BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 400) {
                    getLog().info(
                            String.format(
                                    "Model %s (%s) has been successfully deployed into platform %s with registration key %s",
                                    modelDescriptor.get("name").asText(), modelDescriptor.get("id").asText(), platformUrl,
                                    key));
                } else if (response.statusCode() == 409) {
                    String conflictBody = response.body();
                    String errorCode = readErrorCode(conflictBody);
                    if (!OVERWRITABLE_CONFLICT_ERROR_CODES.contains(errorCode)) {
                        // e.g. the model id is already registered under a different registration key, updating the
                        // registration key used here would only fail with 404 as it was never registered
                        printErrorInfo(conflictBody);
                        throw new IllegalStateException(String.format(
                                "Model deployment of %s failed due to conflict (%s) that cannot be resolved by updating the registration with key %s: %s",
                                modelDescriptor.get("id").asText(), errorCode, key, readErrorMessage(conflictBody)));
                    }
                    if (getPropertyOrParameter(PROP_MODEL_OVERWRITE, overwrite)) {

                        requestURI =
                                URI.create(platformUrl + "/api/platform/v1/models/" + key + "?" + queryString.toString());
                        builder = HttpRequest.newBuilder().uri(requestURI).method("PATCH",
                                BodyPublishers.ofFile(modelDescriptorArchivePath));
                        configureHttpRequest(builder);
                        response = httpClient.send(builder.build(), BodyHandlers.ofString());
                        if (response.statusCode() >= 200 && response.statusCode() < 400) {
                            getLog().info(String.format(
                                    "Model %s (%s) has been successfully updated on platform %s with registration key %s",
                                    modelDescriptor.get("name").asText(), modelDescriptor.get("id").asText(), platformUrl,
                                    key));
                        } else {
                            printErrorInfo(response.body());
                            throw new IllegalStateException(
                                    "Model deployment (override) failed with " + response.statusCode() + " status code: "
                                            + readErrorMessage(response.body()));
                        }
                    } else {
                        printErrorInfo(conflictBody);
                        throw new IllegalStateException(String.format(
                                "Model deployment failed due to conflict (%s), there is already model with that registration key %s use 'overwrite' parameter to update existing: %s",
                                errorCode, key, readErrorMessage(conflictBody)));
                    }
                } else {
                    printErrorInfo(response.body());
                    throw new IllegalStateException("Model deployment failed with " + response.statusCode() + " status code: "
                            + readErrorMessage(response.body()));
                }
            }
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException("Unexpected error while deploying model", e);
        }

    }

    /**
     * Reads the platform error code from an error response body, a response that does not report one is treated as an
     * unknown error.
     */
    private String readErrorCode(String responseBody) {
        String code = readErrorField(responseBody, "code");
        return code == null ? ERROR_CODE_UNKNOWN : code;
    }

    protected void validate(String type) {
        super.validate();

        if (type.equalsIgnoreCase(SHARED_MODEL_TYPE) || type.equalsIgnoreCase(PRIVATE_MODEL_TYPE)) {
            List<String> tenants = getTenants();
            Objects.requireNonNull(tenants, "Tenants are mandatory");
            if (tenants.isEmpty()) {
                throw new IllegalArgumentException("Tenants must be specified (at least one)");
            }
        }
    }

}
