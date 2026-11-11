package ai.timefold.solver.migration.preview;

import java.util.List;

import ai.timefold.solver.migration.AbstractRecipe;

import org.openrewrite.Recipe;
import org.openrewrite.java.ChangeType;

/**
 * Migrates the types of the Timefold Solver service definition module ({@code timefold-solver-service-definition})
 * to its new package structure.
 * <p>
 * The flat {@code ai.timefold.solver.service.definition.api} package and its {@code configuration}, {@code data},
 * {@code domain}, {@code error}, {@code log}, {@code termination} and {@code validation} subpackages
 * were reorganized:
 * <ul>
 * <li>REST payloads (DTOs) moved to {@code api.dto.*},</li>
 * <li>model description types moved to {@code api.description},</li>
 * <li>model conversion types moved to {@code api.conversion},</li>
 * <li>demo data types became a preview API in {@code preview.api.demo},</li>
 * <li>types that are not meant to be used by models moved to {@code internal.*}.</li>
 * </ul>
 * The removed {@code ai.timefold.solver.service.definition.api.UserModel} has no replacement and is not migrated.
 */
public class ServiceDefinitionPackageMigrationRecipe extends AbstractRecipe {

    private static final String API = "ai.timefold.solver.service.definition.api.";
    private static final String NEW_PREVIEW_API = "ai.timefold.solver.service.definition.preview.api.";
    private static final String INTERNAL = "ai.timefold.solver.service.definition.internal.";

    @Override
    public String getDisplayName() {
        return "Migrate the service definition to its new package structure";
    }

    @Override
    public String getDescription() {
        return "Migrate the types of the Timefold Solver service definition to their new packages.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new ChangeType(API + "ModelConvertor", API + "conversion.ModelConvertor", true),
                new ChangeType(API + "ModelConvertorBase", API + "conversion.ModelConvertorBase", true),
                new ChangeType(API + "domain.ConstraintReference", API + "description.ConstraintReference", true),
                new ChangeType(API + "ModelMaturityLevel", API + "description.ModelMaturityLevel", true),
                new ChangeType(API + "domain.Configuration", API + "dto.dataset.Configuration", true),
                new ChangeType(API + "configuration.MapsConfiguration", API + "dto.dataset.MapsConfiguration", true),
                new ChangeType(API + "domain.Metadata", API + "dto.dataset.Metadata", true),
                new ChangeType(API + "domain.ModelConfig", API + "dto.dataset.ModelConfig", true),
                new ChangeType(API + "ModelConfigOverrides", API + "dto.dataset.ModelConfigOverrides", true),
                new ChangeType(API + "ModelInput", API + "dto.dataset.ModelInput", true),
                new ChangeType(API + "ModelOutput", API + "dto.dataset.ModelOutput", true),
                new ChangeType(API + "domain.ModelRequest", API + "dto.dataset.ModelRequest", true),
                new ChangeType(API + "domain.ModelResponse", API + "dto.dataset.ModelResponse", true),
                new ChangeType(API + "configuration.ResourcesConfiguration", API + "dto.dataset.ResourcesConfiguration", true),
                new ChangeType(API + "domain.RunConfiguration", API + "dto.dataset.RunConfiguration", true),
                new ChangeType(API + "SolvingStatus", API + "dto.dataset.SolvingStatus", true),
                new ChangeType(API + "error.ErrorInfo", API + "dto.error.ErrorInfo", true),
                new ChangeType(API + "error.ValidationErrorInfo", API + "dto.error.ValidationErrorInfo", true),
                new ChangeType(API + "log.LogInfo", API + "dto.log.LogInfo", true),
                new ChangeType(API + "ModelInputPatch", API + "dto.patch.ModelInputPatch", true),
                new ChangeType(API + "ModelInputPatchOp", API + "dto.patch.ModelInputPatchOp", true),
                new ChangeType(API + "domain.ModelInputPatchRequest", API + "dto.patch.ModelInputPatchRequest", true),
                new ChangeType(API + "ModelConstraintJustification", API + "dto.scoreanalysis.ModelConstraintJustification",
                        true),
                new ChangeType(API + "domain.ScoreAnalysisConfig", API + "dto.scoreanalysis.ScoreAnalysisConfig", true),
                new ChangeType(API + "domain.ScoreAnalysisRequest", API + "dto.scoreanalysis.ScoreAnalysisRequest", true),
                new ChangeType(API + "termination.SolverTerminationConfig", API + "dto.solve.SolverTerminationConfig", true),
                new ChangeType(API + "validation.AbstractIssue", API + "dto.validation.issue.AbstractIssue", true),
                new ChangeType(API + "validation.Issue", API + "dto.validation.issue.Issue", true),
                new ChangeType(API + "validation.IssueCode", API + "dto.validation.issue.IssueCode", true),
                new ChangeType(API + "validation.IssueSeverity", API + "dto.validation.issue.IssueSeverity", true),
                new ChangeType(API + "validation.IssueType", API + "dto.validation.issue.IssueType", true),
                new ChangeType(API + "validation.metadata.IssueMessage", API + "dto.validation.issue.metadata.IssueMessage",
                        true),
                new ChangeType(API + "validation.IssueMetadata", API + "dto.validation.issue.metadata.IssueMetadata", true),
                new ChangeType(API + "validation.LegacyValidationResult", API + "dto.validation.LegacyValidationResult", true),
                new ChangeType(API + "validation.dto.ValidationIssueTypes", API + "dto.validation.ValidationIssueTypes", true),
                new ChangeType(API + "validation.dto.ValidationResult", API + "dto.validation.ValidationResult", true),
                new ChangeType(API + "validation.ValidationStatus", API + "dto.validation.ValidationStatus", true),
                new ChangeType(API + "domain.DataFormat", API + "metrics.DataFormat", true),
                new ChangeType(API + "validation.ValidationBuilder", API + "validation.model.ValidationBuilder", true),
                new ChangeType(API + "validation.Validated", API + "validation.schema.Validated", true),
                new ChangeType(API + "configuration.ConfigurationProfile", INTERNAL + "configuration.ConfigurationProfile",
                        true),
                new ChangeType(API + "ModelPostProcessor", INTERNAL + "dataset.ModelPostProcessor", true),
                new ChangeType(API + "SolverInput", INTERNAL + "dataset.SolverInput", true),
                new ChangeType(API + "Status", INTERNAL + "dataset.Status", true),
                new ChangeType(API + "ModelDescriptor", INTERNAL + "descriptor.ModelDescriptor", true),
                new ChangeType(API + "Resources", INTERNAL + "descriptor.Resources", true),
                new ChangeType(API + "ResourceType", INTERNAL + "descriptor.ResourceType", true),
                new ChangeType(API + "TrialConfig", INTERNAL + "descriptor.TrialConfig", true),
                new ChangeType(API + "validation.Validator", INTERNAL + "validation.schema.Validator", true),
                new ChangeType(API + "data.AbstractBasicDemoDataGenerator",
                        NEW_PREVIEW_API + "demo.AbstractBasicDemoDataGenerator", true),
                new ChangeType(API + "data.DemoData", NEW_PREVIEW_API + "demo.DemoData", true),
                new ChangeType(API + "data.DemoDataConfigEntry", NEW_PREVIEW_API + "demo.DemoDataConfigEntry", true),
                new ChangeType(API + "data.DemoDataGenerator", NEW_PREVIEW_API + "demo.DemoDataGenerator", true),
                new ChangeType(API + "data.DemoMetaData", NEW_PREVIEW_API + "demo.DemoMetaData", true));
    }
}
