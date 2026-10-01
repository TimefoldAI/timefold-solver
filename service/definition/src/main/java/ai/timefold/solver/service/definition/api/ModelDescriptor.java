package ai.timefold.solver.service.definition.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.timefold.solver.service.definition.internal.descriptor.ConstraintGroupDescriptor;
import ai.timefold.solver.service.definition.internal.descriptor.DocumentationDescriptor;
import ai.timefold.solver.service.definition.internal.descriptor.InputMetricDescriptor;
import ai.timefold.solver.service.definition.internal.descriptor.ModelConfigDescriptor;
import ai.timefold.solver.service.definition.internal.descriptor.OutputMetricDescriptor;
import ai.timefold.solver.service.definition.internal.descriptor.UISupport;
import ai.timefold.solver.service.definition.internal.descriptor.VisualizationPageDescriptor;

public class ModelDescriptor {

    public static final String RESOURCE_NAME = "timefold-model-descriptor.json";

    /* Unique id, composed by model id and version. Ex: employee-scheduling_v1 */
    protected String id;

    /* Model id, identifies a unique model type across versions. Ex: employee-scheduling */
    protected String model;

    /* Model version. Ex: v1 */
    protected String version;

    /* Main API resource type for this model version. Ex: schedules */
    protected ResourceType resourceType;

    /* Human-readable model name */
    protected String name;

    /* Human-readable model long description */
    protected String description;

    protected ModelMaturityLevel maturityLevel;

    protected String imageRef;

    protected String imageRefNative;

    protected Map<String, String> environment = new HashMap<>();

    protected List<String> features = new ArrayList<>();

    protected List<ConstraintGroupDescriptor> constraintGroupDescriptors = new ArrayList<>();

    protected ModelConfigDescriptor modelConfigDescriptor;

    protected List<InputMetricDescriptor> inputMetricDescriptors = new ArrayList<>();

    protected List<OutputMetricDescriptor> outputMetricDescriptors = new ArrayList<>();

    protected Integer maxThreadCount;

    protected String logoUrl;

    protected List<String> images = new ArrayList<>();

    protected TrialConfig trialConfig;

    protected Resources resources;

    protected UISupport uiSupport = UISupport.NONE;

    protected DocumentationDescriptor documentationDescriptor;

    protected List<VisualizationPageDescriptor> visualizationPages = new ArrayList<>();

    protected boolean requiresMap = false;

    /**
     * @return Unique id, composed by model id and version. Ex: employee-scheduling_v1
     */
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return Human-readable model name
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Human-readable model long description
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageRef() {
        return imageRef;
    }

    public void setImageRef(String imageRef) {
        this.imageRef = imageRef;
    }

    public String getImageRefNative() {
        return imageRefNative;
    }

    public void setImageRefNative(String imageRefNative) {
        this.imageRefNative = imageRefNative;
    }

    /**
     * @return Model version. Ex: v1
     */
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }

    public ModelMaturityLevel getMaturityLevel() {
        return maturityLevel;
    }

    public void setMaturityLevel(ModelMaturityLevel maturityLevel) {
        this.maturityLevel = maturityLevel;
    }

    public List<String> getFeatures() {
        return features;
    }

    public void setFeatures(List<String> features) {
        this.features = features;
    }

    public List<ConstraintGroupDescriptor> getConstraintGroupDescriptors() {
        return constraintGroupDescriptors;
    }

    public void setConstraintGroupDescriptors(List<ConstraintGroupDescriptor> constraintGroupDescriptors) {
        this.constraintGroupDescriptors = constraintGroupDescriptors;
    }

    public ModelConfigDescriptor getModelConfigDescriptor() {
        return modelConfigDescriptor;
    }

    public void setModelConfigDescriptor(ModelConfigDescriptor modelConfigDescriptor) {
        this.modelConfigDescriptor = modelConfigDescriptor;
    }

    public List<InputMetricDescriptor> getInputMetricDescriptors() {
        return inputMetricDescriptors;
    }

    public void setInputMetricDescriptors(List<InputMetricDescriptor> inputMetricDescriptors) {
        this.inputMetricDescriptors = inputMetricDescriptors;
    }

    public List<OutputMetricDescriptor> getOutputMetricDescriptors() {
        return outputMetricDescriptors;
    }

    public void setOutputMetricDescriptors(List<OutputMetricDescriptor> outputMetricDescriptors) {
        this.outputMetricDescriptors = outputMetricDescriptors;
    }

    public Integer getMaxThreadCount() {
        return maxThreadCount;
    }

    public void setMaxThreadCount(Integer maxThreadCount) {
        this.maxThreadCount = maxThreadCount;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public TrialConfig getTrialConfig() {
        return trialConfig;
    }

    public void setTrialConfig(TrialConfig trialConfig) {
        this.trialConfig = trialConfig;
    }

    public Resources getResources() {
        return resources;
    }

    public void setResources(Resources resources) {
        this.resources = resources;
    }

    /**
     * @return Model id, identifies a unique model type across versions. Ex: employee-scheduling
     */
    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setUiSupport(UISupport uiSupport) {
        this.uiSupport = uiSupport;
    }

    public UISupport getUiSupport() {
        return uiSupport;
    }

    public DocumentationDescriptor getDocumentationDescriptor() {
        return documentationDescriptor;
    }

    public void setDocumentationDescriptor(DocumentationDescriptor documentationDescriptor) {
        this.documentationDescriptor = documentationDescriptor;
    }

    public List<VisualizationPageDescriptor> getVisualizationPages() {
        return visualizationPages;
    }

    public void setVisualizationPages(List<VisualizationPageDescriptor> visualizationPages) {
        this.visualizationPages = visualizationPages;
    }

    public boolean isRequiresMap() {
        return requiresMap;
    }

    public void setRequiresMap(boolean requiresMap) {
        this.requiresMap = requiresMap;
    }

    public static ModelDescriptor from(ModelDescriptor descriptor) {
        ModelDescriptor copy = new ModelDescriptor();
        copy.setId(descriptor.getId());
        copy.setModel(descriptor.getModel());
        copy.setName(descriptor.getName());
        copy.setDescription(descriptor.getDescription());
        copy.setVersion(descriptor.getVersion());
        copy.setResourceType(descriptor.getResourceType());
        copy.setImageRef(descriptor.getImageRef());
        copy.setImageRefNative(descriptor.getImageRefNative());
        copy.setMaturityLevel(descriptor.getMaturityLevel());
        copy.setFeatures(descriptor.getFeatures());
        copy.setMaxThreadCount(descriptor.getMaxThreadCount());
        copy.setLogoUrl(descriptor.getLogoUrl());
        copy.setImages(descriptor.getImages());
        copy.setConstraintGroupDescriptors(descriptor.getConstraintGroupDescriptors());
        copy.setModelConfigDescriptor(descriptor.getModelConfigDescriptor());
        copy.setInputMetricDescriptors(descriptor.getInputMetricDescriptors());
        copy.setOutputMetricDescriptors(descriptor.getOutputMetricDescriptors());
        copy.setTrialConfig(descriptor.getTrialConfig());
        copy.setResources(descriptor.getResources());
        copy.setUiSupport(descriptor.getUiSupport());
        copy.setDocumentationDescriptor(descriptor.getDocumentationDescriptor());
        copy.setVisualizationPages(descriptor.getVisualizationPages());
        copy.setRequiresMap(descriptor.isRequiresMap());
        return copy;
    }
}
