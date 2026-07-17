package ai.timefold.solver.service.quarkus.deployment;

import static ai.timefold.solver.service.definition.api.rest.OperationId.POST_OPERATIONS_ID_PATTERN;
import static ai.timefold.solver.service.quarkus.deployment.util.ProcessorUtils.excludeType;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Singleton;

import ai.timefold.solver.core.api.score.stream.ConstraintMetaModel;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.impl.util.SolverVersionUtils;
import ai.timefold.solver.service.definition.api.ModelDescriptor;
import ai.timefold.solver.service.definition.api.ModelMaturityLevel;
import ai.timefold.solver.service.definition.api.ResourceType;
import ai.timefold.solver.service.definition.api.Resources;
import ai.timefold.solver.service.definition.api.TrialConfig;
import ai.timefold.solver.service.definition.api.data.AbstractBasicDemoDataGenerator;
import ai.timefold.solver.service.definition.api.data.DemoData;
import ai.timefold.solver.service.definition.api.data.DemoDataGenerator;
import ai.timefold.solver.service.definition.api.description.ConstraintGroupInfo;
import ai.timefold.solver.service.definition.api.description.ConstraintInfo;
import ai.timefold.solver.service.definition.api.domain.ConstraintReference;
import ai.timefold.solver.service.definition.api.domain.DataFormat;
import ai.timefold.solver.service.definition.api.domain.ModelRequest;
import ai.timefold.solver.service.definition.api.rest.OperationId;
import ai.timefold.solver.service.definition.internal.descriptor.ConstraintDescriptor;
import ai.timefold.solver.service.definition.internal.descriptor.ConstraintGroupDescriptor;
import ai.timefold.solver.service.definition.internal.descriptor.DocumentationDescriptor;
import ai.timefold.solver.service.definition.internal.descriptor.DocumentationSupport;
import ai.timefold.solver.service.definition.internal.descriptor.InputMetricDescriptor;
import ai.timefold.solver.service.definition.internal.descriptor.ModelBuildInfo;
import ai.timefold.solver.service.definition.internal.descriptor.ModelConfigDescriptor;
import ai.timefold.solver.service.definition.internal.descriptor.ModelConfigParameter;
import ai.timefold.solver.service.definition.internal.descriptor.OutputMetricDescriptor;
import ai.timefold.solver.service.definition.internal.descriptor.ParameterKind;
import ai.timefold.solver.service.definition.internal.descriptor.UISupport;
import ai.timefold.solver.service.definition.internal.descriptor.VisualizationPageDescriptor;
import ai.timefold.solver.service.jackson.impl.SdkBuildTimeObjectMapperFactory;
import ai.timefold.solver.service.quarkus.deployment.builditem.AdditionalDescriptorFilesBuildItem;
import ai.timefold.solver.service.quarkus.deployment.builditem.ModelArchiveBuildItem;
import ai.timefold.solver.service.quarkus.deployment.builditem.ModelComponentsBuildItem;
import ai.timefold.solver.service.quarkus.deployment.builditem.ModelInfoBuildItem;
import ai.timefold.solver.service.quarkus.deployment.builditem.RestComponentsBuildItem;
import ai.timefold.solver.service.quarkus.deployment.builditem.SingleOpenApiDocumentBuildItem;
import ai.timefold.solver.service.quarkus.deployment.config.VisualizationPagesConfig;
import ai.timefold.solver.service.quarkus.deployment.descriptor.DefaultConstraintGroupDescriptorFactory;
import ai.timefold.solver.service.quarkus.deployment.openapi.SchemaPostProcessor;
import ai.timefold.solver.service.quarkus.deployment.openapi.SchemaUtils;

import org.apache.commons.lang3.exception.UncheckedException;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.Parameter.In;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.eclipse.microprofile.openapi.models.tags.Tag;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.fs.util.ZipUtils;
import io.quarkus.info.deployment.spi.InfoBuildTimeValuesBuildItem;
import io.quarkus.smallrye.openapi.deployment.spi.OpenApiDocumentBuildItem;
import io.smallrye.config.common.utils.StringUtil;
import io.smallrye.openapi.api.SmallRyeOpenAPI;

class TimefoldModelDescriptorProcessor {

    private static final Logger LOG = Logger.getLogger(TimefoldModelDescriptorProcessor.class);

    private static final ObjectMapper MAPPER = SdkBuildTimeObjectMapperFactory.create();

    private static final DotName DEMO_DATA_GENERATOR = DotName.createSimple(DemoDataGenerator.class.getName());
    private static final DotName ABSTRACT_DEMO_DATA_GENERATOR = DotName.createSimple(AbstractBasicDemoDataGenerator.class);
    private static final DotName WITH_MAPS_FACADE =
            DotName.createSimple("ai.timefold.solver.enterprise.service.maps.facade.WithMapsFacadeMarker");

    private static final String MODELS_PATH_PREFIX = "/api/models/";

    private static final String RESPONSE_CODE_OK = "200";

    private static final String RESPONSE_CODE_NO_CONTENT = "204";

    private static final String RESPONSE_CODE_BAD_REQUEST = "400";

    private static final String RESPONSE_CODE_NOT_AUTHENTICATED = "401";

    private static final String RESPONSE_CODE_NOT_AUTHORIZED = "403";

    private static final String RESPONSE_CODE_NOT_FOUND = "404";

    private static final String RESPONSE_CODE_TOO_MANY_REQUESTS = "429";

    private static final List<String> CONFIG_PROPERTIES = List.of(
            "ai.timefold.platform.termination.spent-limit",
            "ai.timefold.platform.termination.maximum-spent-limit",
            "ai.timefold.platform.termination.unimproved-spent-limit",
            "ai.timefold.platform.termination.maximum-unimproved-spent-limit",
            "ai.timefold.platform.map-service.max-distance-from-road");

    private static final String MODEL_MATURITY_LEVEL_PROPERTY = "timefold.model.maturity-level";

    private static final String MODEL_FEATURES_PROPERTY = "timefold.model.features";

    private static final String MODEL_MAX_THREAD_COUNT_PROPERTY = "ai.timefold.model.max-thread-count";

    private static final String APPLICATION_NAME_PROPERTY = "timefold.application.name";
    private static final String APPLICATION_DESCRIPTION_PROPERTY = "timefold.application.description";
    static final String APPLICATION_VERSION_PROPERTY = "timefold.application.version";

    private static final String MODEL_TRIAL_DURATION_PROPERTY = "timefold.model.trial.duration";
    private static final String MODEL_TRIAL_MAX_EXTENSIONS_PROPERTY = "timefold.model.trial.max-extensions";
    private static final String MODEL_TRIAL_EXTENSION_DURATION_PROPERTY = "timefold.model.trial.extension-duration";

    private static final String MEMORY_REQUEST_PROPERTY = "timefold.model.resources.memory.request";
    private static final String MEMORY_LIMIT_PROPERTY = "timefold.model.resources.memory.limit";
    private static final String CPU_REQUEST_PROPERTY = "timefold.model.resources.cpu.request";
    private static final String CPU_LIMIT_PROPERTY = "timefold.model.resources.cpu.limit";

    private static final String DOCUMENTATION_SUPPORT_PROPERTY = "timefold.model.documentation.support";
    private static final String DOCUMENTATION_SOURCE_PROPERTY = "timefold.model.documentation.source";

    private static final String UI_SUPPORT_PROPERTY = "timefold.model.ui-support";
    private static final Path APP_JS_SOURCE_PATH = Path.of("src", "main", "resources", "META-INF", "resources");

    // To avoid depending on the whole OpenAPI, we use the fully qualified class name.
    private static final DotName OPEN_API_SCHEMA_ANNOTATION_FQCN =
            DotName.createSimple("org.eclipse.microprofile.openapi.annotations.media.Schema");

    private static final String DEFAULT_CONSTRAINT_WEIGHT_FIELD_SUFFIX = "Weight";

    private static final String INSIGHTS_DATABASE_NAME = "tf_insights";
    private static final String INSIGHTS_ALLOWED_PATTERN = "^[a-zA-Z0-9_-]+$";
    private static final int INSIGHTS_MAX_NAMESPACE_LENGTH_BYTES = 255;

    VisualizationPagesConfig visualizationPagesConfig;

    @BuildStep
    @Produce(ArtifactResultBuildItem.class)
    void generateModelDescriptorArchive(Optional<ModelArchiveBuildItem> modelArchiveBuildItem,
            OutputTargetBuildItem out) {
        if (modelArchiveBuildItem.isPresent()) {

            try {
                Path modelArchivePath = modelArchiveBuildItem.get().getArchiveContentPath();
                Path descriptor = modelArchivePath.resolve("timefold-model-descriptor.json");
                if (!Files.exists(descriptor)) {
                    throw new IllegalStateException(
                            "Model descriptor not found");
                }
                ModelDescriptor model = MAPPER.readValue(descriptor.toFile(), ModelDescriptor.class);

                if (io.quarkus.runtime.util.StringUtil.isNullOrEmpty(model.getId())
                        || io.quarkus.runtime.util.StringUtil.isNullOrEmpty(model.getImageRef())) {
                    throw new IllegalStateException(
                            "Model loaded from " + modelArchivePath + " in not valid (missing name and/or image ref");
                }
                // check if there is OpenAPI definition for the model
                Path openAPIDefinition = modelArchivePath.resolve(model.getId() + "/openapi/service.json");
                if (!Files.exists(openAPIDefinition)) {
                    throw new IllegalStateException(
                            "Model loaded from " + modelArchivePath + " in not valid (missing OpenAPI definition file)");
                }
                // copy enhanced OpenAPI spec file to target folder for validation purpose
                Path enhancedOpenApiPath =
                        Paths.get(out.getOutputDirectory().toString(), "timefold-model-enhanced-openapi.json");
                Files.copy(openAPIDefinition, enhancedOpenApiPath, StandardCopyOption.REPLACE_EXISTING);

                // check if there is JSON schema definition for the schedule operation
                Path jsonSchemaDefinition =
                        modelArchivePath.resolve(model.getId() + "/jsonschema/schedule.json");
                if (!Files.exists(jsonSchemaDefinition)) {
                    throw new IllegalStateException(
                            "Model loaded from " + modelArchivePath + " in not valid (missing JSON Schema definition file)");
                }

                // check if there is default config profile
                Path configProfileDefinition =
                        modelArchivePath.resolve(model.getId() + "/default-config-profile/default-config.json");
                if (!Files.exists(configProfileDefinition)) {
                    throw new IllegalStateException(
                            "Model loaded from " + modelArchivePath
                                    + " in not valid (missing default config profile definition file)");
                }

                Path modelDescriptor = Paths.get(out.getOutputDirectory().toString(), "model-descriptor.zip");

                ZipUtils.zip(modelArchivePath,
                        modelDescriptor);

                LOG.debug("Model archive content is located in " + out.getOutputDirectory());
            } catch (Exception e) {
                throw new UncheckedException(e);
            }
        }
    }

    @BuildStep
    void selectSingleOpenApiDocumentBuildItem(List<OpenApiDocumentBuildItem> openApiDocs,
            BuildProducer<SingleOpenApiDocumentBuildItem> singleOpenApiDocProducer) {
        singleOpenApiDocProducer.produce(SingleOpenApiDocumentBuildItem.fromMultiple(openApiDocs));
    }

    @BuildStep
    void buildModelInfo(ApplicationInfoBuildItem appInfo, SingleOpenApiDocumentBuildItem openApiDoc,
            BuildProducer<ModelInfoBuildItem> producer) {
        OpenAPI openAPI = openApiDoc.getOpenApiDocument().get();

        String model = appInfo.getName();
        String modelId = modelId(model, openAPI.getInfo().getVersion());

        producer.produce(new ModelInfoBuildItem(modelId, model));
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    void generateValidationForModel(CombinedIndexBuildItem combinedIndex,
            ModelInfoBuildItem modelInfo, ModelComponentsBuildItem modelComponentsBuildItem,
            RestComponentsBuildItem restComponentsBuildItem,
            SingleOpenApiDocumentBuildItem openApiDoc, OutputTargetBuildItem out,
            BuildProducer<ModelArchiveBuildItem> producer,
            BuildProducer<GeneratedResourceBuildItem> resourceBuildItemBuildProducer,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResourcesProducer,
            List<InfoBuildTimeValuesBuildItem> infoBuildTimeItems)
            throws Exception {

        OpenAPI openAPI = openApiDoc.getOpenApiDocument().get();

        byte[] jsonDocument =
                SmallRyeOpenAPI.builder().withInitialModel(openAPI).build().toJSON().getBytes(StandardCharsets.UTF_8);

        generateSchemaForPOSTOperations(POST_OPERATIONS_ID_PATTERN, openAPI, modelInfo.getModelId(), jsonDocument,
                null, resourceBuildItemBuildProducer, nativeImageResourcesProducer);

        generateModelDescriptor(modelInfo.getModelId(), modelInfo.getModelName(), openAPI, out.getOutputDirectory(),
                restComponentsBuildItem.getRestResource(),
                modelComponentsBuildItem,
                combinedIndex,
                resourceBuildItemBuildProducer,
                nativeImageResourcesProducer);
    }

    @BuildStep(onlyIfNot = IsDevelopment.class)
    void preparePlatformArchiveForModel(CombinedIndexBuildItem combinedIndex, ModelInfoBuildItem modelInfo,
            RestComponentsBuildItem restComponentsBuildItem,
            ModelComponentsBuildItem modelComponentsBuildItem,
            SingleOpenApiDocumentBuildItem openApiDoc,
            // To define the correct order between build steps producing additional files inside the model descriptor
            // directory structure.
            List<AdditionalDescriptorFilesBuildItem> additionalDescriptorFilesBuildItem,
            OutputTargetBuildItem out,
            BuildProducer<ModelArchiveBuildItem> producer,
            BuildProducer<GeneratedResourceBuildItem> resourceBuildItemBuildProducer,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResourcesProducer)
            throws Exception {

        OpenAPI openAPI = openApiDoc.getOpenApiDocument().get();

        if (restComponentsBuildItem.getRestResource().isPresent()) {
            generateOpenAPIAndSchema(modelInfo.getModelName(), modelInfo.getModelId(), openAPI, out.getOutputDirectory(),
                    resourceBuildItemBuildProducer, nativeImageResourcesProducer);
        }

        generateDemoData(out, modelInfo.getModelId(), combinedIndex);

        // create descriptor itself
        generateModelDescriptor(modelInfo.getModelId(), modelInfo.getModelName(), openAPI, out.getOutputDirectory(),
                restComponentsBuildItem.getRestResource(),
                modelComponentsBuildItem,
                combinedIndex,
                resourceBuildItemBuildProducer,
                nativeImageResourcesProducer);

        producer.produce(new ModelArchiveBuildItem(Paths.get(out.getOutputDirectory().toString(), "timefold")));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void generateBuildTimeInfoBean(ModelBuildInfoRecorder recorder,
            OutputTargetBuildItem out,
            List<InfoBuildTimeValuesBuildItem> infobuildTimeItems,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) throws Exception {

        Map<String, InfoBuildTimeValuesBuildItem> infoItemsByName =
                infobuildTimeItems.stream().collect(Collectors.toMap(InfoBuildTimeValuesBuildItem::getName, info -> info));

        InfoBuildTimeValuesBuildItem buildInfo = infoItemsByName.get("build");
        InfoBuildTimeValuesBuildItem gitInfo = infoItemsByName.get("git");

        String solverVersion =
                SolverVersionUtils.bareVersion(SolverVersionUtils.CORE_GIT_PROPERTIES, SolverFactory.class);
        String sdkVersion = SolverVersionUtils.bareVersion(ModelDescriptor.class);
        String version = buildInfo != null ? (String) buildInfo.getValue().get("version") : null;
        String buildTime = buildInfo != null ? (String) buildInfo.getValue().get("time") : null;
        if (buildTime != null) {
            buildTime = OffsetDateTime.parse(buildTime).truncatedTo(ChronoUnit.MILLIS).withOffsetSameInstant(ZoneOffset.UTC)
                    .toString();
        }
        String branch = gitInfo != null ? (String) gitInfo.getValue().get("branch") : null;
        String commit =
                gitInfo != null ? (String) ((Map<String, Object>) gitInfo.getValue().getOrDefault("commit", Map.of())).get("id")
                        : null;

        ModelBuildInfo modelBuildInfo = new ModelBuildInfo(solverVersion, sdkVersion, version, buildTime, branch, commit);

        byte[] buildInfoContent = MAPPER.writeValueAsBytes(modelBuildInfo);
        Path buildInfoFile = Paths.get(out.getOutputDirectory().toString(), "timefold", "build-info.json");

        Files.createDirectories(buildInfoFile.getParent());
        Files.write(buildInfoFile, buildInfoContent);

        syntheticBeanBuildItemBuildProducer.produce(
                SyntheticBeanBuildItem.configure(ModelBuildInfo.class)
                        .scope(Singleton.class)
                        .unremovable()
                        .supplier(recorder.create(modelBuildInfo))
                        .done());
    }

    /*
     * Helper methods
     */
    private void generateSchemaForPOSTOperations(String operationIdPattern, OpenAPI openAPI, String modelId,
            byte[] jsonSchemaDocument, Path directory,
            BuildProducer<GeneratedResourceBuildItem> resourceBuildItemBuildProducer,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResourcesProducer) throws IOException {
        List<PathItem> schedulePathItems = openAPI.getPaths().getPathItems().values().stream()
                .filter(pathItem -> pathItem.getPOST() != null
                        && pathItem.getPOST().getOperationId().matches(operationIdPattern))
                .toList();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode types = mapper.readTree(jsonSchemaDocument).get("components").get("schemas");

        for (PathItem schedulePathItem : schedulePathItems) {

            String operationId = schedulePathItem.getPOST().getOperationId();

            RequestBody requestBody = schedulePathItem.getPOST().getRequestBody();

            // "POST /{id}" has no request body, as it only triggers solving of an existing dataset.
            if (requestBody == null) {
                LOG.debug("Operation " + operationId + " does not have a request body defined, skipping schema generation.");
                continue;
            }

            String operationBodyDataTypeRef =
                    requestBody.getContent().getMediaType("application/json")
                            .getSchema().getRef();

            if (operationBodyDataTypeRef == null) {
                LOG.warn("Operation " + operationId + " does not have a ref data type for operation "
                        + requestBody.getContent().getMediaType("application/json"));
                continue;
            }

            String[] elements = operationBodyDataTypeRef.split("/");
            String schemaName = elements[elements.length - 1];

            JsonNode schemaDef = types.get(schemaName);

            Map<String, JsonNode> masterByType = new HashMap<>();
            Map<String, List<JsonNode>> delayedByType = new HashMap<>();

            schemaDef = resolveRefs(schemaDef, types, masterByType, delayedByType);
            schemaDef = SchemaPostProcessor.removeDiscriminators(schemaDef);

            for (Entry<String, JsonNode> p : masterByType.entrySet()) {
                List<JsonNode> delayed = delayedByType.get(p.getKey());

                if (delayed != null) {

                    for (JsonNode d : delayed) {
                        for (Entry<String, JsonNode> property : p.getValue().properties()) {

                            ((ObjectNode) d).set(property.getKey(), property.getValue().deepCopy());
                        }
                    }
                }
            }

            ((ObjectNode) schemaDef).put("additionalProperties", false);
            ((ObjectNode) schemaDef).put("minProperties", 1);

            String name = operationId + ".json";
            byte[] schemaDocument = mapper.writeValueAsBytes(schemaDef);

            if (directory != null) {
                if (!Files.exists(directory)) {
                    Files.createDirectories(directory);
                }

                Files.write(Paths.get(directory.toString(), name), schemaDocument);
                LOG.debug("Generated resource with json schema " + name);

            }

            String resourceName = modelId + "/jsonschema/" + name;
            resourceBuildItemBuildProducer.produce(new GeneratedResourceBuildItem(resourceName, schemaDocument));
            nativeImageResourcesProducer.produce(new NativeImageResourceBuildItem(resourceName));
        }
    }

    private JsonNode resolveRefs(JsonNode schemaDef, JsonNode schemas, Map<String, JsonNode> processed,
            Map<String, List<JsonNode>> delayedByType) {

        List<JsonNode> refs = schemaDef.findParents("$ref");

        for (JsonNode withRef : refs) {
            JsonNode ref = ((ObjectNode) withRef).remove("$ref");

            String[] elements = ref.asText().split("/");
            String schemaName = elements[elements.length - 1];
            JsonNode refSchema = schemas.get(schemaName);
            if (!processed.containsKey(ref.asText())) {
                processed.put(ref.asText(), withRef);
                for (Entry<String, JsonNode> property : refSchema.properties()) {

                    JsonNode prop = resolveRefs(property.getValue(), schemas, processed, delayedByType);

                    ((ObjectNode) withRef).set(property.getKey(), prop);
                }

            } else {
                delayedByType.compute(ref.asText(), (k, v) -> {
                    if (v == null) {
                        v = new ArrayList<>();
                    }
                    v.add(withRef);
                    return v;
                });
            }
        }

        return schemaDef;
    }

    protected static void validateModelId(String modelId) {
        if (modelId == null || modelId.trim().isEmpty()) {
            throw new IllegalArgumentException("Model name cannot be null or empty.");
        }

        if (!modelId.matches(INSIGHTS_ALLOWED_PATTERN)) {
            throw new IllegalArgumentException(
                    "Model name can only contain letters (a-z, A-Z) and numbers (0-9), and hyphens (-).");
        }

        long underscoreCount = modelId.chars().filter(ch -> ch == '_').count();
        if (underscoreCount > 1) {
            throw new IllegalArgumentException("Model name cannot contain underscore (_).");
        }

        String fullNamespace = INSIGHTS_DATABASE_NAME + "." + modelId;
        int byteLength = fullNamespace.getBytes(StandardCharsets.UTF_8).length;
        if (byteLength > INSIGHTS_MAX_NAMESPACE_LENGTH_BYTES) {
            throw new IllegalArgumentException("Model name and version is too long.");
        }
    }

    protected static void validateApplicationVersion(String version) {
        if (version == null || version.trim().isEmpty()) {
            throw new IllegalArgumentException("""
                    The application version is missing.
                    Set the '%s' property (e.g. in application.properties) so the build can proceed."""
                    .formatted(APPLICATION_VERSION_PROPERTY));
        }
    }

    private void generateModelDescriptor(String modelId, String model, OpenAPI openAPI,
            Path outputDirectory,
            Optional<ClassInfo> restResource,
            ModelComponentsBuildItem modelComponentsBuildItem,
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<GeneratedResourceBuildItem> resourceBuildItemBuildProducer,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResourcesProducer)
            throws IOException {

        validateModelId(modelId);

        Config config = ConfigProvider.getConfig();

        ModelDescriptor descriptor = new ModelDescriptor();
        descriptor.setId(modelId);
        descriptor.setModel(model);
        descriptor.setName(
                config.getOptionalValue(APPLICATION_NAME_PROPERTY, String.class).orElse(openAPI.getInfo().getTitle()));
        String applicationVersion = config.getOptionalValue(APPLICATION_VERSION_PROPERTY, String.class).orElse(null);
        validateApplicationVersion(applicationVersion);
        descriptor.setVersion(applicationVersion);
        descriptor.setResourceType(getResourceTypeFromRestResource(restResource));
        descriptor.setDescription(config.getOptionalValue(APPLICATION_DESCRIPTION_PROPERTY, String.class)
                .orElse(openAPI.getInfo().getDescription()));

        StringBuilder imageRefBuilder = new StringBuilder();

        Optional<String> completeImageRef = config.getOptionalValue("quarkus.container-image.image", String.class);
        if (completeImageRef.isPresent()) {
            imageRefBuilder.append(completeImageRef.get());
        } else {

            config.getOptionalValue("quarkus.container-image.registry", String.class)
                    .ifPresent(val -> imageRefBuilder.append(val).append("/"));
            config.getOptionalValue("quarkus.container-image.group", String.class)
                    .ifPresent(val -> imageRefBuilder.append(val).append("/"));
            imageRefBuilder.append(
                    config.getOptionalValue("quarkus.container-image.name", String.class)
                            .orElse(config.getValue("quarkus.application.name", String.class)))
                    .append(":");
            imageRefBuilder.append(
                    config.getOptionalValue("quarkus.container-image.tag", String.class)
                            .orElse(config.getValue("quarkus.application.version", String.class)));
        }
        descriptor.setImageRef(imageRefBuilder.toString());

        Optional<String> nativeImageSuffix = config.getOptionalValue("image.native-suffix", String.class);
        String[] origValues = descriptor.getImageRef().split(":");
        descriptor.setImageRefNative(origValues[0] + nativeImageSuffix.orElse("") + ":" + origValues[1]);

        List<InputMetricDescriptor> inputMetricDescriptors =
                inputMetricsFromClassInfo(modelComponentsBuildItem.getModelInputMetrics());
        descriptor.setInputMetricDescriptors(inputMetricDescriptors);

        List<OutputMetricDescriptor> metrics =
                outputMetricsFromClassInfo(combinedIndex.getIndex(), modelComponentsBuildItem.getModelOutputMetrics());
        descriptor.setOutputMetricDescriptors(metrics);

        // check model specific data
        String modelMaturityLevel =
                config.getOptionalValue(MODEL_MATURITY_LEVEL_PROPERTY, String.class)
                        .orElse(ModelMaturityLevel.Experimental.name());
        descriptor.setMaturityLevel(ModelMaturityLevel.valueOf(modelMaturityLevel));

        List<String> features = config.getOptionalValues(MODEL_FEATURES_PROPERTY, String.class).orElse(new ArrayList<>());
        descriptor.setFeatures(features);

        List<ConstraintGroupDescriptor> constraintGroupDescriptors = createConstraintGroupDescriptors(
                modelComponentsBuildItem.getConstraintMetaModel());
        descriptor.setConstraintGroupDescriptors(constraintGroupDescriptors);

        ModelConfigDescriptor modelConfigDescriptor =
                modelConfigDescriptorFromModelConfigOverrideClass(modelComponentsBuildItem.getModelConfigOverrides());
        descriptor.setModelConfigDescriptor(modelConfigDescriptor);

        int maxThreads = config.getOptionalValue(MODEL_MAX_THREAD_COUNT_PROPERTY, Integer.class)
                .orElse(1);
        descriptor.setMaxThreadCount(maxThreads);

        Optional<Integer> trialDuration = config.getOptionalValue(MODEL_TRIAL_DURATION_PROPERTY, Integer.class);
        Optional<Integer> trialMaxExtensions = config.getOptionalValue(MODEL_TRIAL_MAX_EXTENSIONS_PROPERTY, Integer.class);
        Optional<Integer> trialExtensionDuration =
                config.getOptionalValue(MODEL_TRIAL_EXTENSION_DURATION_PROPERTY, Integer.class);

        descriptor.setTrialConfig(new TrialConfig(trialDuration.orElse(null), trialMaxExtensions.orElse(null),
                trialExtensionDuration.orElse(null)));

        boolean requiresMap = combinedIndex.getIndex().getClassByName(WITH_MAPS_FACADE) != null;
        descriptor.setRequiresMap(requiresMap);

        Optional<String> memoryRequest = config.getOptionalValue(MEMORY_REQUEST_PROPERTY, String.class);
        Optional<String> memoryLimit = config.getOptionalValue(MEMORY_LIMIT_PROPERTY, String.class);
        Optional<String> cpuRequest = config.getOptionalValue(CPU_REQUEST_PROPERTY, String.class);
        Optional<String> cpuLimit = config.getOptionalValue(CPU_LIMIT_PROPERTY, String.class);

        boolean resourcesExist =
                memoryRequest.isPresent() || memoryLimit.isPresent() || cpuRequest.isPresent() || cpuLimit.isPresent();

        if (resourcesExist) {
            Resources resources = new Resources(memoryRequest.orElse(null), memoryLimit.orElse(null), cpuRequest.orElse(null),
                    cpuLimit.orElse(null));
            descriptor.setResources(resources);
        }

        Map<String, String> environment = new HashMap<>();

        for (String property : CONFIG_PROPERTIES) {
            config.getOptionalValue(property, String.class)
                    .ifPresent(val -> environment.put(StringUtil.replaceNonAlphanumericByUnderscores(property).toUpperCase(),
                            val));
        }
        descriptor.setEnvironment(environment);
        String resourceName = ModelDescriptor.RESOURCE_NAME;

        processModelImages(descriptor, outputDirectory);
        processLegacyModelUI(descriptor, outputDirectory);
        descriptor.setDocumentationDescriptor(processModelDocumentation());
        descriptor.setVisualizationPages(processVisualizationPages());

        byte[] content = MAPPER.writeValueAsBytes(descriptor);
        if (outputDirectory != null) {
            // save model descriptor to file system
            Path descriptorFile = Paths.get(outputDirectory.toString(), "timefold", resourceName);
            Files.createDirectories(descriptorFile.getParent());
            Files.write(descriptorFile, content);
        }

        resourceBuildItemBuildProducer.produce(new GeneratedResourceBuildItem(resourceName, content));
        nativeImageResourcesProducer.produce(new NativeImageResourceBuildItem(resourceName));
    }

    private ResourceType getResourceTypeFromRestResource(Optional<ClassInfo> restResource) {
        if (restResource.isEmpty()) {
            throw new IllegalStateException(
                    "Could not derive model resource type: no REST interface extending ModelRest was found.");
        }
        AnnotationInstance pathAnnotation = restResource.get().annotations(jakarta.ws.rs.Path.class).stream()
                .filter(a -> a.target().kind() == AnnotationTarget.Kind.CLASS)
                .findFirst()
                .orElse(null);
        if (pathAnnotation == null || pathAnnotation.value() == null) {
            throw new IllegalStateException(
                    "Could not derive model resource type: ModelRest interface is missing @Path.");
        }
        return getResourceTypeFromPath(pathAnnotation.value().asString());
    }

    protected static ResourceType getResourceTypeFromPath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            throw new IllegalStateException(
                    "Could not derive model resource type: ModelRest @Path value is empty.");
        }

        String normalizedPath = rawPath.startsWith("/") ? rawPath.substring(1) : rawPath;
        String[] segments = normalizedPath.split("/");
        if (segments.length == 0 || segments[0].isBlank()) {
            throw new IllegalStateException(
                    "Could not derive model resource type: ModelRest @Path does not contain path segments.");
        }
        if (segments[0].matches("v\\d+.*")) {
            if (segments.length > 1 && !segments[1].isBlank()) {
                return new ResourceType(segments[1]);
            }
            throw new IllegalStateException(
                    "Could not derive model resource type: ModelRest @Path only contains API version but no resource segment.");
        }
        return new ResourceType(segments[0]);
    }

    private List<ConstraintGroupDescriptor> createConstraintGroupDescriptors(ConstraintMetaModel constraintMetaModel) {
        if (constraintMetaModel == null) {
            LOG.debug("No constraint meta model found.");
            return Collections.emptyList();
        }

        List<ConstraintDescriptor> constraintDescriptorsWithoutGroup = new ArrayList<>();
        Map<ConstraintGroupInfo, List<ConstraintDescriptor>> constraintDescriptionsByGroup = new LinkedHashMap<>();
        for (var constraint : constraintMetaModel.getConstraints()) {
            if (constraint.getConstraintMetadata() instanceof ConstraintInfo constraintInfo) {
                var constraintDescriptor = new ConstraintDescriptor(constraintInfo.id(), constraintInfo.name(),
                        constraintInfo.description(), constraint.getConstraintWeight().toString());
                if (constraintInfo.constraintGroup() == null) {
                    constraintDescriptorsWithoutGroup.add(constraintDescriptor);
                } else {
                    constraintDescriptionsByGroup
                            .computeIfAbsent(constraintInfo.constraintGroup(), ignored -> new ArrayList<>())
                            .add(constraintDescriptor);
                }
            } else {
                LOG.debug(
                        "Constraint (%s) metadata is not compatible with %s".formatted(constraint.getConstraintMetadata().id(),
                                ConstraintInfo.class.getSimpleName()));
            }
        }

        List<ConstraintGroupDescriptor> constraintGroupDescriptors =
                new ArrayList<>(constraintDescriptionsByGroup.size());

        for (var entry : constraintDescriptionsByGroup.entrySet()) {
            ConstraintDescriptor[] constraintDescriptors = entry.getValue().toArray(new ConstraintDescriptor[0]);
            ConstraintGroupInfo constraintGroupInfo = entry.getKey();
            ConstraintGroupDescriptor constraintGroupDescriptor =
                    new ConstraintGroupDescriptor(constraintGroupInfo.id(), constraintGroupInfo.name(),
                            constraintGroupInfo.description(), constraintGroupInfo.icon(), constraintDescriptors,
                            constraintGroupInfo.tags());
            constraintGroupDescriptors.add(constraintGroupDescriptor);
        }

        if (!constraintDescriptorsWithoutGroup.isEmpty()) {
            constraintGroupDescriptors.add(DefaultConstraintGroupDescriptorFactory.create(constraintDescriptorsWithoutGroup));
        }

        return constraintGroupDescriptors;
    }

    private void generateOpenAPIAndSchema(String model, String modelId, OpenAPI openAPI, Path outputDirectory,
            BuildProducer<GeneratedResourceBuildItem> resourceBuildItemBuildProducer,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResourcesProducer)
            throws IOException {
        openAPI.setServers(List.of(OASFactory.createServer().url(MODELS_PATH_PREFIX + model)));

        List<Tag> tags = new ArrayList<>(openAPI.getTags());

        // remove extra tags
        tags.removeIf(tag -> tag.getName().equals("Service information"));
        openAPI.setTags(tags);

        // add security based on api key and responses in case of failed or missing authentication
        openAPI.getPaths().getPathItems().values().forEach(pi -> pi.getOperations().values()
                .forEach(operation -> {
                    operation.addSecurityRequirement(OASFactory.createSecurityRequirement().addScheme("auth"));

                    addResponse(RESPONSE_CODE_NOT_AUTHENTICATED, "Missing or invalid API key", operation);

                    addResponse(RESPONSE_CODE_NOT_AUTHORIZED,
                            "API key used with this call does not have access to the operation", operation);

                    addResponseWithError(RESPONSE_CODE_TOO_MANY_REQUESTS, "Too many requests or other policy violations",
                            operation);
                }));

        SecurityScheme security = OASFactory.createSecurityScheme();
        security.setType(SecurityScheme.Type.APIKEY);
        security.setIn(SecurityScheme.In.HEADER);
        security.setName("X-API-KEY");
        security.setDescription("API key required to authenticate as part of Timefold Platform");
        openAPI.getComponents().setSecuritySchemes(Map.of("auth", security));

        openAPI.getPaths().getPathItems().values().stream()
                .filter(pathItem -> pathItem.getPOST() != null
                        && pathItem.getPOST().getOperationId().matches(POST_OPERATIONS_ID_PATTERN)
                        && pathItem.getPOST().getParameters() != null && pathItem.getPOST().getParameters().stream()
                                // filter out operations that are id based (represent run) as it should not have configuration id (profile) available
                                // as it should use the run config itself to keep it consistent
                                .noneMatch(param -> param.getIn().equals(In.PATH) && param.getName().equalsIgnoreCase("id")))
                .forEach(item -> {
                    // add configuration id query parameter to allow to select config for given request
                    item.getPOST().addParameter(OASFactory.createParameter().name("configurationId").in(In.QUERY)
                            .required(false)
                            .description(
                                    "Optional identifier of the configuration profile. You may provide either the configuration profile’s unique ID or its name.")
                            .schema(OASFactory.createSchema().type(List.of(SchemaType.STRING))));

                    // add queue priority for given request
                    item.getPOST().addParameter(OASFactory.createParameter().name("priority").in(In.QUERY)
                            .required(false)
                            .description(
                                    "Optional solve priority. Defaults to 5, minimum 0 (lowest priority), maximum 10 (highest priority).")
                            .schema(OASFactory.createSchema().type(List.of(SchemaType.INTEGER))));

                    // add tags query parameter to allow appending additional tags
                    item.getPOST().addParameter(OASFactory.createParameter().name("tags").in(In.QUERY)
                            .required(false)
                            .description(
                                    "Optional tags to be associated with dataset")
                            .schema(OASFactory.createSchema().type(List.of(SchemaType.ARRAY))
                                    .items(OASFactory.createSchema().type(List.of(SchemaType.STRING)))));
                });
        // add configuration id and tags to the from-intput and from-patch endpoints
        openAPI.getPaths().getPathItems().values().stream()
                .filter(pathItem -> pathItem.getPOST() != null
                        && (pathItem.getPOST().getOperationId().equals(OperationId.FROM_INPUT)
                                || pathItem.getPOST().getOperationId().equals(OperationId.FROM_PATCH)))
                .forEach(item -> {
                    // add configuration id query parameter to allow to select config for given request
                    item.getPOST().addParameter(OASFactory.createParameter().name("configurationId").in(In.QUERY)
                            .required(false)
                            .description(
                                    "Optional identifier of the configuration profile. You may provide either the configuration profile’s unique ID or its name.")
                            .schema(OASFactory.createSchema().type(List.of(SchemaType.STRING))));

                    // add queue priority for given request
                    item.getPOST().addParameter(OASFactory.createParameter().name("priority").in(In.QUERY)
                            .required(false)
                            .description(
                                    "Optional solve priority. Defaults to 5, minimum 0 (lowest priority), maximum 10 (highest priority).")
                            .schema(OASFactory.createSchema().type(List.of(SchemaType.INTEGER))));

                    // add tags query parameter to allow to appending additional tags
                    item.getPOST().addParameter(OASFactory.createParameter().name("tags").in(In.QUERY)
                            .required(false)
                            .description(
                                    "Optional tags to be associated with dataset")
                            .schema(OASFactory.createSchema().type(List.of(SchemaType.ARRAY))
                                    .items(OASFactory.createSchema().type(List.of(SchemaType.STRING)))));
                });

        // add priority to the solve endpoint
        openAPI.getPaths().getPathItems().values().stream()
                .filter(pathItem -> pathItem.getPOST() != null
                        && pathItem.getPOST().getOperationId().equals(OperationId.SOLVE_DATASET))
                .forEach(item -> {
                    // add queue priority for given request
                    item.getPOST().addParameter(OASFactory.createParameter().name("priority").in(In.QUERY)
                            .required(false)
                            .description(
                                    "Optional solve priority. Defaults to 5, minimum 0 (lowest priority), maximum 10 (highest priority).")
                            .schema(OASFactory.createSchema().type(List.of(SchemaType.INTEGER))));
                });

        openAPI.getPaths().getPathItems().values().stream()
                .filter(pathItem -> pathItem.getDELETE() != null
                        && pathItem.getDELETE().getOperationId().matches(OperationId.TERMINATE_SCHEDULE))
                .forEach(item -> {
                    // add configuration id query parameter to allow to select config for given request
                    item.getDELETE().addParameter(OASFactory.createParameter().name("force").in(In.QUERY)
                            .required(false)
                            .description(
                                    "Instructs to forcibly terminate in case regular termination fails")
                            .schema(OASFactory.createSchema().type(List.of(SchemaType.BOOLEAN))));
                });

        Optional<PathItem> mainResourcePathItem = openAPI.getPaths().getPathItems().values().stream()
                .filter(pathItem -> pathItem.getGET() != null
                        && Objects.equals(pathItem.getGET().getOperationId(), OperationId.GET_SCHEDULE))
                .findFirst();

        // add restore operation to the model api
        mainResourcePathItem.ifPresent(this::addRestoreOperation);

        Optional<PathItem> runResourcePathItem = openAPI.getPaths().getPathItems().values().stream()
                .filter(pathItem -> pathItem.getGET() != null
                        && Objects.equals(pathItem.getGET().getOperationId(), OperationId.GET_SCHEDULE_STATUS))
                .findFirst();

        // add run update name and tags operation
        runResourcePathItem.ifPresent(this::addUpdateRunNameAndTagsOperation);

        Optional<PathItem> metadataResourcePathItem = openAPI.getPaths().getPathItems().values().stream()
                .filter(pathItem -> pathItem.getGET() != null
                        && Objects.equals(pathItem.getGET().getOperationId(), OperationId.GET_METADATA))
                .findFirst();

        // add metadata update name and tags operation
        metadataResourcePathItem.ifPresent(this::addUpdateMetadadaNameAndTagsOperation);

        Optional<Entry<String, PathItem>> terminateResourcePath = openAPI.getPaths().getPathItems().entrySet().stream()
                .filter(entry -> entry.getValue().getDELETE() != null
                        && Objects.equals(entry.getValue().getDELETE().getOperationId(), OperationId.TERMINATE_SCHEDULE))
                .findFirst();

        // add delete run operation
        terminateResourcePath.ifPresent(path -> this.addDeleteRunOperation(openAPI, terminateResourcePath.get().getKey(),
                terminateResourcePath.get().getValue()));

        Path directory = Paths.get(outputDirectory.toString(), "timefold", modelId, "openapi");

        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }

        SmallRyeOpenAPI smallRyeOpenAPI = SmallRyeOpenAPI.builder().withInitialModel(openAPI).build();
        String name = "service.json";
        byte[] jsonSchemaDocument = smallRyeOpenAPI.toJSON().getBytes(StandardCharsets.UTF_8);
        Path file = Paths.get(directory.toString(), name);
        Files.write(file, jsonSchemaDocument);
        LOG.debug("Generated resource with OpenAPI augmented definition " + name);

        name = "service.yaml";
        byte[] yamlSchemaDocument = smallRyeOpenAPI.toYAML().getBytes(StandardCharsets.UTF_8);
        file = Paths.get(directory.toString(), name);
        Files.write(file, yamlSchemaDocument);
        LOG.debug("Generated resource with OpenAPI augmented definition " + name);

        // generate JSON schema for matching POST operations
        generateSchemaForPOSTOperations(POST_OPERATIONS_ID_PATTERN, openAPI, modelId, jsonSchemaDocument,
                Paths.get(outputDirectory.toString(), "timefold", modelId, "jsonschema"), resourceBuildItemBuildProducer,
                nativeImageResourcesProducer);
    }

    private void addUpdateRunNameAndTagsOperation(PathItem runResourcePathItem) {
        Operation updateOperation = OASFactory.createOperation();
        updateOperation.addTag("Administration");
        updateOperation.setOperationId("updateSchedule");
        updateOperation.setDescription("Update item name and tags if still available");
        updateOperation.setSummary("(Deprecated endpoint, please use /{id}/metadata instead) Update item name and tags");
        updateOperation.setDeprecated(true);
        updateOperation
                .setRequestBody(OASFactory.createRequestBody()
                        .content(OASFactory.createContent().addMediaType("application/json",
                                OASFactory.createMediaType().schema(
                                        OASFactory.createSchema().addType(SchemaType.OBJECT)
                                                .addProperty("name",
                                                        OASFactory.createSchema().type(List.of(SchemaType.STRING)))
                                                .addProperty("tags", OASFactory.createSchema()
                                                        .type(List.of(SchemaType.ARRAY))
                                                        .uniqueItems(true)
                                                        .items(OASFactory.createSchema().addType(SchemaType.STRING)))))));

        Parameter updateIdParameter = OASFactory.createParameter();
        updateIdParameter.setName("id");
        updateIdParameter.setIn(In.PATH);
        updateIdParameter.setDescription("Unique id of the item to be updated");
        updateIdParameter.setRequired(true);
        updateIdParameter.setSchema(OASFactory.createSchema().type(List.of((SchemaType.STRING))));

        updateOperation.addParameter(updateIdParameter);

        updateOperation.addSecurityRequirement(OASFactory.createSecurityRequirement().addScheme("auth"));

        APIResponse response = OASFactory.createAPIResponse();
        response.setDescription("Dataset successfully updated.");
        response.setContent(OASFactory.createContent().addMediaType("application/json",
                OASFactory.createMediaType().schema(OASFactory.createSchema().ref("#/components/schemas/Metadata"))));
        APIResponses responses = OASFactory.createAPIResponses();
        responses.addAPIResponse(RESPONSE_CODE_OK, response);
        updateOperation.setResponses(responses);

        addResponse(RESPONSE_CODE_NOT_FOUND, "Dataset not found.", updateOperation);

        addResponse(RESPONSE_CODE_BAD_REQUEST, "Dataset can not be updated, invalid state.", updateOperation);

        addResponse(RESPONSE_CODE_NOT_AUTHENTICATED, "Missing or invalid API key", updateOperation);

        addResponse(RESPONSE_CODE_NOT_AUTHORIZED,
                "API key used with this call does not have access to the operation", updateOperation);

        runResourcePathItem.setPATCH(updateOperation);
    }

    private void addUpdateMetadadaNameAndTagsOperation(PathItem runResourcePathItem) {
        Operation updateOperation = OASFactory.createOperation();
        updateOperation.addTag("Administration");
        updateOperation.setOperationId("updateMetadata");
        updateOperation.setDescription("Update dataset name and tags if still available");
        updateOperation.setSummary("Update dataset name and tags");
        updateOperation
                .setRequestBody(OASFactory.createRequestBody()
                        .content(OASFactory.createContent().addMediaType("application/json",
                                OASFactory.createMediaType().schema(
                                        OASFactory.createSchema().addType(SchemaType.OBJECT)
                                                .addProperty("name",
                                                        OASFactory.createSchema().type(List.of(SchemaType.STRING)))
                                                .addProperty("tags", OASFactory.createSchema()
                                                        .type(List.of(SchemaType.ARRAY))
                                                        .uniqueItems(true)
                                                        .items(OASFactory.createSchema().addType(SchemaType.STRING)))))));

        Parameter updateIdParameter = OASFactory.createParameter();
        updateIdParameter.setName("id");
        updateIdParameter.setIn(In.PATH);
        updateIdParameter.setDescription("Unique id of the dataset to be updated.");
        updateIdParameter.setRequired(true);
        updateIdParameter.setSchema(OASFactory.createSchema().type(List.of((SchemaType.STRING))));

        updateOperation.addParameter(updateIdParameter);

        updateOperation.addSecurityRequirement(OASFactory.createSecurityRequirement().addScheme("auth"));

        APIResponse response = OASFactory.createAPIResponse();
        response.setDescription("Dataset successfully updated.");
        response.setContent(OASFactory.createContent().addMediaType("application/json",
                OASFactory.createMediaType().schema(OASFactory.createSchema().ref("#/components/schemas/Metadata"))));
        APIResponses responses = OASFactory.createAPIResponses();
        responses.addAPIResponse(RESPONSE_CODE_OK, response);
        updateOperation.setResponses(responses);

        addResponse(RESPONSE_CODE_NOT_FOUND, "Dataset not found.", updateOperation);

        addResponse(RESPONSE_CODE_BAD_REQUEST, "Dataset can not be updated, invalid state.", updateOperation);

        addResponse(RESPONSE_CODE_NOT_AUTHENTICATED, "Missing or invalid API key.", updateOperation);

        addResponse(RESPONSE_CODE_NOT_AUTHORIZED,
                "API key used with this call does not have access to the operation.", updateOperation);

        runResourcePathItem.setPATCH(updateOperation);
    }

    private void addRestoreOperation(PathItem mainResourcePathItem) {
        Operation restoreOperation = OASFactory.createOperation();
        restoreOperation.addTag("Administration");
        restoreOperation.setOperationId("restoreSchedule");
        restoreOperation.setDescription("Restores previously deleted dataset if it is still available.");
        restoreOperation.setSummary("Restores dataset that was previously deleted.");

        Parameter idParameter = OASFactory.createParameter();
        idParameter.setName("id");
        idParameter.setIn(In.PATH);
        idParameter.setDescription("Unique id of the dataset to be restored.");
        idParameter.setSchema(OASFactory.createSchema().type(List.of((SchemaType.STRING))));
        idParameter.setRequired(true);
        restoreOperation.addParameter(idParameter);

        restoreOperation.addSecurityRequirement(OASFactory.createSecurityRequirement().addScheme("auth"));

        restoreOperation.setResponses(OASFactory.createAPIResponses());

        addResponse(RESPONSE_CODE_NO_CONTENT, "Item successfully restored.", restoreOperation);

        addResponse(RESPONSE_CODE_NOT_FOUND, "Item cannot be restored or was not found.", restoreOperation);

        addResponse(RESPONSE_CODE_NOT_AUTHENTICATED, "Missing or invalid API key.", restoreOperation);

        addResponse(RESPONSE_CODE_NOT_AUTHORIZED,
                "API key used with this call does not have access to the operation.", restoreOperation);

        mainResourcePathItem.setPUT(restoreOperation);
    }

    private void addDeleteRunOperation(OpenAPI openAPI, String basePath, PathItem pathItem) {

        PathItem purgePathItem = OASFactory.createPathItem();
        Operation deleteRunOperation = OASFactory.createOperation();
        deleteRunOperation.addTag("Administration");
        deleteRunOperation.setOperationId("deleteSchedule");
        deleteRunOperation.setDescription("Delete dataset from storage and return the dataset with given identifier.");
        deleteRunOperation.setSummary("Delete dataset from storage.");

        Parameter idParameter = OASFactory.createParameter();
        idParameter.setName("id");
        idParameter.setIn(In.PATH);
        idParameter.setDescription("Unique id of the item to be deleted.");
        idParameter.setSchema(OASFactory.createSchema().type(List.of((SchemaType.STRING))));
        idParameter.setRequired(true);
        deleteRunOperation.addParameter(idParameter);

        deleteRunOperation.addSecurityRequirement(OASFactory.createSecurityRequirement().addScheme("auth"));

        APIResponse response = OASFactory.createAPIResponse();
        response.setDescription("Returns scheduling with given identifier.");
        response.setContent(pathItem.getDELETE().getResponses().getAPIResponse(RESPONSE_CODE_OK).getContent());
        APIResponses responses = OASFactory.createAPIResponses();
        responses.addAPIResponse(RESPONSE_CODE_OK, response);
        deleteRunOperation.setResponses(responses);

        addResponse(RESPONSE_CODE_BAD_REQUEST, "In case dataset is still solving.", deleteRunOperation);

        addResponse(RESPONSE_CODE_NOT_FOUND, "In case the requested dataset does not exist.", deleteRunOperation);

        addResponse(RESPONSE_CODE_NOT_AUTHENTICATED, "Missing or invalid API key.", deleteRunOperation);

        addResponse(RESPONSE_CODE_NOT_AUTHORIZED,
                "API key used with this call does not have access to the operation.", deleteRunOperation);

        purgePathItem.setDELETE(deleteRunOperation);
        openAPI.getPaths().addPathItem(basePath + "/purge", purgePathItem);
    }

    private void addResponse(String code, String description, Operation operation) {
        APIResponse notAuthenticated = OASFactory.createAPIResponse();
        notAuthenticated.setDescription(description);
        operation.getResponses().addAPIResponse(code, notAuthenticated);
    }

    private void addResponseWithError(String code, String description, Operation operation) {
        APIResponse notAuthenticated = OASFactory.createAPIResponse();
        notAuthenticated.setDescription(description);
        Schema schema = OASFactory.createSchema();
        schema.setRef("#/components/schemas/ErrorInfo");

        MediaType mediaType = OASFactory.createMediaType();
        mediaType.setSchema(schema);

        Content content = OASFactory.createContent();
        content.addMediaType("application/json", mediaType);
        notAuthenticated.setContent(content);
        operation.getResponses().addAPIResponse(code, notAuthenticated);
    }

    private String modelId(String model, String version) {
        if (model.endsWith(version)) {
            return model;
        }

        return model + "_" + version;
    }

    private void generateDemoData(OutputTargetBuildItem out, String modelId, CombinedIndexBuildItem combinedIndex)
            throws Exception {

        Collection<ClassInfo> generators = excludeType(combinedIndex.getIndex().getAllKnownImplementations(DEMO_DATA_GENERATOR),
                ABSTRACT_DEMO_DATA_GENERATOR);

        Path directory = Paths.get(out.getOutputDirectory().toString(), "timefold", modelId, "demo-data");

        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }

        for (ClassInfo generator : generators) {
            try {
                Class<?> clazz =
                        Class.forName(generator.name().toString(), true, Thread.currentThread().getContextClassLoader());

                DemoDataGenerator demoDataGenerator = (DemoDataGenerator) clazz.getDeclaredConstructor().newInstance();
                List<DemoData> generated = demoDataGenerator.generateDemoData();

                for (DemoData demoData : generated) {
                    String demoDataId = demoData.metaData().id();
                    Path file = Paths.get(directory.toString(), demoDataId);
                    ModelRequest<?, ?> demoDataRequest = demoData.modelRequest();
                    Files.write(file, MAPPER.writeValueAsBytes(demoDataRequest));
                    LOG.debug("DemoData " + demoDataId + " saved: " + file);

                    // store demo data as model input
                    file = Paths.get(directory.toString(), demoDataId + "_input");
                    Files.write(file, MAPPER.writeValueAsBytes(demoDataRequest.modelInput()));
                }

                Files.write(Paths.get(directory.toString(), "demo-keys"),
                        MAPPER.writeValueAsBytes(generated.stream().map(demoData -> demoData.metaData()).toArray()));
            } catch (Exception e) {
                LOG.error(
                        "Unable to collect demo data from class " + generator.name().toString() + " message " + e.getMessage(),
                        e);
            }
        }
    }

    private void processModelImages(ModelDescriptor descriptor, Path outputDirectory) throws IOException {
        if (outputDirectory != null) {
            // list of image prefixes that should be included in descriptor but not added as images
            List<String> includedPrefixes = List.of("og-");
            // copy model images and logo
            Path imagesPath = Paths.get(outputDirectory.getParent().toString(), "model-images");

            if (Files.exists(imagesPath)) {
                String apiPath = "/api/info/models/" + descriptor.getId() + "/model-images/";
                try (Stream<Path> stream = Files.list(imagesPath)) {
                    stream.forEach(image -> {
                        String fileName = image.toFile().getName();
                        Path destinationPath =
                                Paths.get(outputDirectory.toString(), "timefold", descriptor.getId(), "model-images", fileName);
                        try {
                            Files.createDirectories(destinationPath.getParent());
                            Files.copy(image, destinationPath, StandardCopyOption.REPLACE_EXISTING);

                            if (includedPrefixes.stream().anyMatch(prefix -> fileName.startsWith(prefix))) {
                                LOG.debugv("Image %s is included as standalone image", fileName);
                            } else if (fileName.equals("logo.png")) {
                                descriptor.setLogoUrl(apiPath + fileName);
                            } else {
                                descriptor.getImages().add(apiPath + fileName);
                            }
                        } catch (IOException e) {
                            LOG.error("Error processing image: " + fileName, e);
                        }

                    });
                }
                Collections.sort(descriptor.getImages());
            }
        }
    }

    private void processLegacyModelUI(ModelDescriptor descriptor, Path outputDirectory) throws IOException {
        Optional<UISupport> uiSupportParam = ConfigProvider.getConfig()
                .getOptionalValue(UI_SUPPORT_PROPERTY, UISupport.class);

        if (uiSupportParam.isPresent()) {
            UISupport uiSupport = uiSupportParam.get();
            if (uiSupport == UISupport.NONE) {
                // If the property is set to NONE, skip processing.
                LOG.debug("UI support declared by the property (%s) is NONE. Skipping UI resources processing."
                        .formatted(UI_SUPPORT_PROPERTY));
                descriptor.setUiSupport(UISupport.NONE);
                return;
            }

            if (uiSupport == UISupport.APP_JS) {
                LOG.debug("UI support declared by the property (%s) is (%s). Processing UI resources."
                        .formatted(UI_SUPPORT_PROPERTY, UISupport.APP_JS));
                boolean success = processModelAppJsUI(descriptor, outputDirectory);
                if (!success) {
                    throw new IllegalStateException("The model's UI resources were not found in the expected location (%s)."
                            .formatted(APP_JS_SOURCE_PATH.toString()));
                }
                descriptor.setUiSupport(UISupport.APP_JS);
            }
        } else { // If not configured, try detecting the APP_JS UI resources.
            LOG.debug("UI support is not configured by the property (%s). Detecting the UI resources.");
            boolean success = processModelAppJsUI(descriptor, outputDirectory);
            if (success) {
                descriptor.setUiSupport(UISupport.APP_JS);
            } else {
                LOG.debug("The model's UI resources were not found in the expected location (%s). Assuming no UI is supported."
                        .formatted(APP_JS_SOURCE_PATH.toString()));
                descriptor.setUiSupport(UISupport.NONE);
            }
        }
    }

    private boolean processModelAppJsUI(ModelDescriptor descriptor, Path outputDirectory) throws IOException {
        Objects.requireNonNull(outputDirectory);

        // copy model's ui resources
        Path uiResourcesPath = Paths.get(outputDirectory.getParent().toString(), APP_JS_SOURCE_PATH.toString());
        boolean uiResourcesFound = Files.exists(uiResourcesPath);

        if (uiResourcesFound) {
            try (Stream<Path> stream = Files.walk(uiResourcesPath)) {
                stream.forEach(resourceFile -> {
                    String fileName = uiResourcesPath.relativize(resourceFile).toString();
                    if (!fileName.isEmpty()) {
                        Path destinationPath =
                                Paths.get(outputDirectory.toString(), "timefold", descriptor.getId(), "ui", fileName);
                        try {
                            Files.createDirectories(destinationPath.getParent());
                            if (Files.isDirectory(destinationPath)) {
                                Files.createDirectories(destinationPath);
                            }
                            Files.copy(resourceFile, destinationPath, StandardCopyOption.REPLACE_EXISTING);

                        } catch (IOException e) {
                            throw new UncheckedIOException("Unexpected IO exception while collecting model's UI resources",
                                    e);
                        }
                    }
                });
            }
        }

        return uiResourcesFound;
    }

    private List<InputMetricDescriptor> inputMetricsFromClassInfo(ClassInfo classInfo) {
        List<InputMetricDescriptor> inputMetricDescriptors = new ArrayList<>();

        for (FieldInfo fieldInfo : classInfo.fields()) {
            InputMetricDescriptor inputMetricDescriptor = inputMetricDescriptorFromField(fieldInfo);
            if (inputMetricDescriptor != null) {
                inputMetricDescriptors.add(inputMetricDescriptor);
            }
        }
        inputMetricDescriptors.sort(Comparator.comparing(InputMetricDescriptor::priority));
        return inputMetricDescriptors;
    }

    private InputMetricDescriptor inputMetricDescriptorFromField(FieldInfo field) {
        if (isConstantField(field)) {
            return null;
        }

        Optional<AnnotationInstance> schemaAnnotationOptional = getSchemaAnnotation(field);
        if (schemaAnnotationOptional.isEmpty()) {
            return new InputMetricDescriptor(field.name(), field.name(), "", DataFormat.fromString(field.type().toString()), 1,
                    "", "");
        }

        AnnotationInstance schemaAnnotation = schemaAnnotationOptional.get();

        String name = valueOfAnnotationAttributeOrDefault(schemaAnnotation.value("name"), field.name());
        String title = valueOfAnnotationAttributeOrDefault(schemaAnnotation.value("title"), field.name());
        String description = valueOfAnnotationAttributeOrDefault(schemaAnnotation.value("description"), "");
        String type = valueOfAnnotationAttributeOrDefault(schemaAnnotation.value("format"),
                valueOfAnnotationAttributeOrDefault(schemaAnnotation.value("type"), field.type().toString()).toLowerCase());
        String example = valueOfAnnotationAttributeOrDefault(schemaAnnotation.value("example"), "");
        int priority = 1;
        String exampleFormated = "";

        AnnotationValue extensions = schemaAnnotation.value("extensions");

        if (extensions != null) {
            AnnotationInstance[] extensionAnnotations = extensions.asNestedArray();
            if (extensionAnnotations != null && extensionAnnotations.length > 0) {
                for (AnnotationInstance extensionAnnotation : extensionAnnotations) {
                    AnnotationValue value = extensionAnnotation.value("name");
                    if (value != null && value.asString().equalsIgnoreCase("x-tf-priority")) {
                        priority =
                                Integer.parseInt(valueOfAnnotationAttributeOrDefault(extensionAnnotation.value("value"), "1"));
                    }
                    if (value != null && value.asString().equalsIgnoreCase("x-tf-example")) {
                        exampleFormated = valueOfAnnotationAttributeOrDefault(extensionAnnotation.value("value"), "");
                    }
                }
            }
        }
        return new InputMetricDescriptor(name, title, description, DataFormat.fromString(type), priority, example,
                exampleFormated);
    }

    private List<OutputMetricDescriptor> outputMetricsFromClassInfo(IndexView index, ClassInfo classInfo) {
        List<OutputMetricDescriptor> outputMetricDescriptors = new ArrayList<>();

        for (FieldInfo field : classInfo.fields()) {

            OutputMetricDescriptor metric = outputMetricDescriptorFromField(field);
            if (metric != null) {
                outputMetricDescriptors.add(metric);
            }
        }
        DotName superClassName = classInfo.superName();

        if (superClassName != null && !superClassName.local().equals(Object.class.getName())) {
            // process fields of super class

            ClassInfo superClassInfo = index.getClassByName(superClassName);
            if (superClassInfo != null) {
                for (FieldInfo field : superClassInfo.fields()) {

                    OutputMetricDescriptor metric = outputMetricDescriptorFromField(field);
                    if (metric != null) {
                        outputMetricDescriptors.add(metric);
                    }
                }
            }
        }
        outputMetricDescriptors.sort(Comparator.comparing(OutputMetricDescriptor::priority));
        return outputMetricDescriptors;
    }

    public static boolean isConstantField(FieldInfo field) {
        return Modifier.isStatic(field.flags()) &&
                Modifier.isFinal(field.flags());
    }

    private OutputMetricDescriptor outputMetricDescriptorFromField(FieldInfo field) {
        if (isConstantField(field)) {
            return null;
        }

        Optional<AnnotationInstance> schemaAnnotationOptional = getSchemaAnnotation(field);
        if (schemaAnnotationOptional.isEmpty()) {
            return new OutputMetricDescriptor(field.name(), field.name(), "", DataFormat.fromString(field.type().toString()), 1,
                    "", "");
        }

        AnnotationInstance schemaAnnotation = schemaAnnotationOptional.get();

        String name = valueOfAnnotationAttributeOrDefault(schemaAnnotation.value("name"), field.name());
        String title = valueOfAnnotationAttributeOrDefault(schemaAnnotation.value("title"), field.name());
        String description = valueOfAnnotationAttributeOrDefault(schemaAnnotation.value("description"), "");
        String type = valueOfAnnotationAttributeOrDefault(schemaAnnotation.value("format"),
                valueOfAnnotationAttributeOrDefault(schemaAnnotation.value("type"), field.type().toString()).toLowerCase());
        String example = valueOfAnnotationAttributeOrDefault(schemaAnnotation.value("example"), "");
        int priority = 1;
        String exampleFormated = "";

        AnnotationValue extensions = schemaAnnotation.value("extensions");

        if (extensions != null) {
            AnnotationInstance[] extensionAnnotations = extensions.asNestedArray();
            if (extensionAnnotations != null && extensionAnnotations.length > 0) {
                for (AnnotationInstance extensionAnnotation : extensionAnnotations) {
                    AnnotationValue value = extensionAnnotation.value("name");
                    if (value != null && value.asString().equalsIgnoreCase("x-tf-priority")) {
                        priority =
                                Integer.parseInt(valueOfAnnotationAttributeOrDefault(extensionAnnotation.value("value"), "1"));
                    }
                    if (value != null && value.asString().equalsIgnoreCase("x-tf-example")) {
                        exampleFormated = valueOfAnnotationAttributeOrDefault(extensionAnnotation.value("value"), "");
                    }
                }
            }
        }

        return new OutputMetricDescriptor(name, title, description, DataFormat.fromString(type), priority, example,
                exampleFormated);
    }

    private String valueOfAnnotationAttributeOrDefault(AnnotationValue value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value.asString();
    }

    private Optional<AnnotationInstance> getSchemaAnnotation(FieldInfo fieldInfo) {
        return getAnnotation(fieldInfo, OPEN_API_SCHEMA_ANNOTATION_FQCN);
    }

    private Optional<AnnotationInstance> getAnnotation(FieldInfo fieldInfo, DotName annotation) {
        Collection<AnnotationInstance> annotationInstances =
                fieldInfo.annotations(annotation.toString()).stream()
                        .filter(annotationInstance -> annotationInstance.target().equals(fieldInfo)).toList();

        if (annotationInstances.isEmpty()) {
            return Optional.empty();
        }

        if (annotationInstances.size() > 1) {
            throw new IllegalStateException(
                    "Field " + fieldInfo.name() + " in class " + fieldInfo.declaringClass().name().toString()
                            + " has more than one " + annotation.withoutPackagePrefix() + " annotations");
        }

        return Optional.of(annotationInstances.iterator().next());
    }

    private ModelConfigDescriptor modelConfigDescriptorFromModelConfigOverrideClass(ClassInfo configOverridesClassInfo) {
        if (configOverridesClassInfo == null) {
            return null;
        }

        // the OpenAPI schema type is derived from the class name.
        final String schemaType = resolveSchemaTypeName(configOverridesClassInfo);

        List<ModelConfigParameter> modelConfigParameters = new ArrayList<>();
        for (FieldInfo fieldInfo : configOverridesClassInfo.fieldsInDeclarationOrder()) {
            if (Modifier.isStatic(fieldInfo.flags())) { // skip constants
                continue;
            }
            ModelConfigParameter modelConfigParameter = resolveModelConfigParameter(fieldInfo);
            modelConfigParameters.add(modelConfigParameter);
        }

        return new ModelConfigDescriptor(schemaType, modelConfigParameters);
    }

    private String resolveSchemaTypeName(ClassInfo classInfo) {
        return classInfo.name().withoutPackagePrefix().toString();
    }

    private ModelConfigParameter resolveModelConfigParameter(FieldInfo fieldInfo) {
        String name = null;
        String title = null;
        String description = null;
        SchemaType type = null;
        DataFormat format = null;
        boolean nullable = false;
        Optional<AnnotationInstance> schemaAnnotationOptional = getSchemaAnnotation(fieldInfo);
        if (schemaAnnotationOptional.isPresent()) {
            AnnotationInstance schemaAnnotation = schemaAnnotationOptional.get();
            name = valueOfAnnotationAttributeOrDefault(schemaAnnotation.value("name"), null);
            title = valueOfAnnotationAttributeOrDefault(schemaAnnotation.value("title"), null);
            description = valueOfAnnotationAttributeOrDefault(schemaAnnotation.value("description"), null);
            AnnotationValue schemaTypeAnnotationValue = schemaAnnotation.value("type");
            type =
                    schemaTypeAnnotationValue != null ? SchemaType.valueOf(schemaTypeAnnotationValue.asString()) : null;
            AnnotationValue formatAnnotationValue = schemaAnnotation.value("format");
            format = formatAnnotationValue != null ? DataFormat.fromString(formatAnnotationValue.asString()) : null;
            AnnotationValue nullableAnnotationValue = schemaAnnotation.value("nullable");
            nullable = nullableAnnotationValue != null && nullableAnnotationValue.asBoolean();
        }

        name = name != null ? name : fieldInfo.name();
        title = title != null ? title : name;
        type = type != null ? type : SchemaUtils.resolveSchemaType(fieldInfo.type());
        if (format == null && SchemaUtils.isDuration(fieldInfo.type())) {
            format = DataFormat.Duration;
        }
        String schemaTypeRef = SchemaUtils.resolveSchemaTypeRef(fieldInfo);

        SchemaType arrayItemType = type == SchemaType.ARRAY ? SchemaUtils.resolveArrayItemSchemaType(fieldInfo) : null;
        Optional<AnnotationInstance> constraintReferenceAnnotationOptional =
                getAnnotation(fieldInfo, DotName.createSimple(ConstraintReference.class));
        String constraintRef = null;
        ParameterKind kind =
                name.endsWith(DEFAULT_CONSTRAINT_WEIGHT_FIELD_SUFFIX) ? ParameterKind.WEIGHT : ParameterKind.PARAMETER;
        if (constraintReferenceAnnotationOptional.isPresent()) {
            AnnotationInstance constraintReferenceAnnotation = constraintReferenceAnnotationOptional.get();
            constraintRef = constraintReferenceAnnotation.value().asString();
            AnnotationValue kindAnnotationValue = constraintReferenceAnnotation.value(ConstraintReference.KIND_FIELD);
            kind = kindAnnotationValue != null ? ParameterKind.valueOf(kindAnnotationValue.asEnum()) : ParameterKind.WEIGHT;
        }

        return new ModelConfigParameter(name, title, description, kind, type, arrayItemType, format, schemaTypeRef,
                nullable, constraintRef);
    }

    private DocumentationDescriptor processModelDocumentation() {
        Config config = ConfigProvider.getConfig();

        DocumentationSupport documentationSupport = config
                .getOptionalValue(DOCUMENTATION_SUPPORT_PROPERTY, DocumentationSupport.class).orElse(DocumentationSupport.NONE);

        if (documentationSupport == DocumentationSupport.EXTERNAL) {
            String source = config.getOptionalValue(DOCUMENTATION_SOURCE_PROPERTY, String.class)
                    .orElseThrow(() -> new IllegalStateException("External documentation source is not set."));

            try {
                return new DocumentationDescriptor(documentationSupport, new URL(source));
            } catch (MalformedURLException e) {
                throw new IllegalStateException("Invalid external documentation source URL (%s).".formatted(source), e);
            }
        }

        return DocumentationDescriptor.none();
    }

    private List<VisualizationPageDescriptor> processVisualizationPages() {
        return toVisualizationPageDescriptors(visualizationPagesConfig);
    }

    static List<VisualizationPageDescriptor> toVisualizationPageDescriptors(VisualizationPagesConfig config) {
        return config.pages().stream()
                .map(page -> new VisualizationPageDescriptor(page.key(), page.icon(), page.label()))
                .toList();
    }

}
