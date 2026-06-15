package ai.timefold.solver.service.quarkus.deployment.validation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

import ai.timefold.solver.service.definition.api.validation.AbstractIssue;
import ai.timefold.solver.service.definition.api.validation.IssueCode;
import ai.timefold.solver.service.definition.api.validation.IssueType;
import ai.timefold.solver.service.definition.impl.validation.ValidationIssueTypeCatalog;
import ai.timefold.solver.service.jackson.impl.SdkBuildTimeObjectMapperFactory;
import ai.timefold.solver.service.quarkus.deployment.builditem.AdditionalDescriptorFilesBuildItem;
import ai.timefold.solver.service.quarkus.deployment.builditem.ModelInfoBuildItem;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.runtime.RuntimeValue;

public final class ValidationIssueTypeCatalogProcessor {

    private static final Logger LOG = Logger.getLogger(ValidationIssueTypeCatalogProcessor.class);

    private static final String VALIDATION_ISSUE_TYPES_FILE_NAME = "validation-issue-types.json";

    @BuildStep
    public DiscoveredIssueTypesBuildItem discoverIssueTypes(
            CombinedIndexBuildItem combinedIndexBuildItem) throws Exception {

        var indexView = combinedIndexBuildItem.getIndex();
        var issueClasses = indexView.getAllKnownSubclasses(AbstractIssue.class);
        var contextClassLoader = Thread.currentThread().getContextClassLoader();

        Map<IssueCode, List<String>> issueCodeToClassNames = new LinkedHashMap<>();
        List<IssueType> issueTypes = new ArrayList<>();

        for (ClassInfo issueClassInfo : issueClasses) {
            if (issueClassInfo.isAbstract()) { // Interested only in instantiable classes.
                continue;
            }
            if (issueClassInfo.name().packagePrefixName()
                    .startsWith(DotName.createSimple("ai.timefold.solver.service.definition"))) {
                continue; // Exclude SDK internal errors
            }
            if (!issueClassInfo.hasNoArgsConstructor()) {
                throw new IllegalStateException("Missing no-arg constructor for class " + issueClassInfo.name());
            }
            Class<?> clazz = contextClassLoader.loadClass(issueClassInfo.name().toString());
            AbstractIssue instance = (AbstractIssue) clazz.getDeclaredConstructor().newInstance();
            IssueType issueType = new IssueType(instance.getCode(), instance.getSeverity(), instance.getMetadata());
            issueTypes.add(issueType);
            issueCodeToClassNames.computeIfAbsent(issueType.code(), ignored -> new ArrayList<>()).add(clazz.getName());
        }

        assertDuplicateIssueTypes(issueCodeToClassNames);
        return new DiscoveredIssueTypesBuildItem(issueTypes);
    }

    @BuildStep(onlyIfNot = IsDevelopment.class)
    public void saveIssueTypesToModelDescriptor(DiscoveredIssueTypesBuildItem discoveredIssueTypes,
            ModelInfoBuildItem modelInfo,
            OutputTargetBuildItem out,
            BuildProducer<AdditionalDescriptorFilesBuildItem> additionalDescriptorFilesProducer) throws Exception {

        Path directory = Paths.get(out.getOutputDirectory().toString(), "timefold", modelInfo.getModelId(),
                "validation");

        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }
        var issueTypes = discoveredIssueTypes.getIssueTypes();
        var mapper = SdkBuildTimeObjectMapperFactory.create();
        byte[] bytes = mapper.writeValueAsBytes(issueTypes);
        Path issueTypesPath = Paths.get(directory.toString(), VALIDATION_ISSUE_TYPES_FILE_NAME);
        Files.write(issueTypesPath, bytes);

        additionalDescriptorFilesProducer.produce(new AdditionalDescriptorFilesBuildItem(issueTypesPath));
        LOG.debug("Generated validation issue types catalog with " + issueTypes.size() + " issue types");
    }

    /**
     * Fails fast if there are multiple {@link AbstractIssue} subclasses sharing the same {@link IssueCode}.
     *
     * @param issueCodeToClassNames {@link Map} of {@link IssueCode} to a list of classes extending the {@link AbstractIssue}
     *        that declare the issue code.
     */
    private void assertDuplicateIssueTypes(Map<IssueCode, List<String>> issueCodeToClassNames) {
        Map<IssueCode, List<String>> duplicates = issueCodeToClassNames.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));

        if (duplicates.isEmpty()) {
            return;
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (var entry : issueCodeToClassNames.entrySet()) {
            if (entry.getValue().size() > 1) { // duplicate
                stringBuilder.append("IssueCode (%s) was found in %d classes (%s)"
                        .formatted(entry.getKey(), entry.getValue().size(),
                                String.join(", ", entry.getValue())))
                        .append("\n");
            }
        }

        if (!stringBuilder.isEmpty()) {
            throw new IllegalStateException("Detected duplicate issue type codes." + "\n" + stringBuilder);
        }
    }

    /**
     * Records a {@link ValidationIssueTypeCatalog} bean to be available at runtime and injected to the REST endpoint.
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void registerValidationIssueTypeCatalogBean(DiscoveredIssueTypesBuildItem discoveredIssueTypes,
            ValidationIssueTypeCatalogRecorder recorder,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {

        RuntimeValue<ValidationIssueTypeCatalog> catalog = recorder.createCatalog(discoveredIssueTypes.getIssueTypes());

        syntheticBeans.produce(SyntheticBeanBuildItem
                .configure(ValidationIssueTypeCatalog.class)
                .scope(ApplicationScoped.class)
                .unremovable()
                .runtimeValue(catalog)
                .done());
    }
}
