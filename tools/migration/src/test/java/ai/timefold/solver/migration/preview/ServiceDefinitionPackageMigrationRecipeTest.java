package ai.timefold.solver.migration.preview;

import static org.openrewrite.java.Assertions.java;

import java.util.List;

import ai.timefold.solver.migration.AbstractRecipeTest;
import ai.timefold.solver.migration.NoWildCardImportStyle;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.TypeValidation;

class ServiceDefinitionPackageMigrationRecipeTest extends AbstractRecipeTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(new ServiceDefinitionPackageMigrationRecipe())
                .typeValidationOptions(TypeValidation.builder().allowMissingType(ignore -> true).build())
                .parser(JavaParser.fromJavaVersion()
                        .styles(List.of(new NoWildCardImportStyle()))
                        // We must add all old classes as stubs to the JavaParser classpath
                        .dependsOn(
                                "package ai.timefold.solver.service.definition.api; public interface ModelInput {}",
                                "package ai.timefold.solver.service.definition.api; public interface ModelOutput {}",
                                "package ai.timefold.solver.service.definition.api; public interface ModelConfigOverrides {}",
                                "package ai.timefold.solver.service.definition.api; public interface ModelConvertor<Solution_, Input_, Output_> {}",
                                "package ai.timefold.solver.service.definition.api; public interface ModelConstraintJustification {}",
                                "package ai.timefold.solver.service.definition.api; public enum ModelMaturityLevel { STABLE }",
                                "package ai.timefold.solver.service.definition.api; public enum SolvingStatus { SOLVING_ACTIVE }",
                                "package ai.timefold.solver.service.definition.api; public record ResourceType(String value) {}",
                                "package ai.timefold.solver.service.definition.api.domain; public record ModelRequest<Input_, Config_>(Input_ modelInput) {}",
                                "package ai.timefold.solver.service.definition.api.domain; public record ConstraintReference(String id) {}",
                                "package ai.timefold.solver.service.definition.api.domain; public enum DataFormat { INTEGER }",
                                "package ai.timefold.solver.service.definition.api.configuration; public record MapsConfiguration(String provider) {}",
                                "package ai.timefold.solver.service.definition.api.termination; public record SolverTerminationConfig(String spentLimit) {}",
                                "package ai.timefold.solver.service.definition.api.validation; public interface Issue {}",
                                "package ai.timefold.solver.service.definition.api.validation; public abstract class AbstractIssue implements Issue {}",
                                "package ai.timefold.solver.service.definition.api.validation; public enum IssueSeverity { ERROR }",
                                "package ai.timefold.solver.service.definition.api.validation; public class ValidationBuilder {}",
                                "package ai.timefold.solver.service.definition.api.validation.dto; public record ValidationResult(String status) {}",
                                "package ai.timefold.solver.service.definition.api.validation.metadata; public record IssueMessage(String message) {}",
                                "package ai.timefold.solver.service.definition.api.data; public record DemoData(Object metaData, Object modelRequest) {}",
                                "package ai.timefold.solver.service.definition.api.data; public interface DemoDataGenerator {}"));
    }

    @Test
    void migrateModelTypes() {
        rewriteRun(java(
                """
                        package timefold;

                        import ai.timefold.solver.service.definition.api.ModelConfigOverrides;
                        import ai.timefold.solver.service.definition.api.ModelConstraintJustification;
                        import ai.timefold.solver.service.definition.api.ModelConvertor;
                        import ai.timefold.solver.service.definition.api.ModelInput;
                        import ai.timefold.solver.service.definition.api.ModelMaturityLevel;
                        import ai.timefold.solver.service.definition.api.ModelOutput;
                        import ai.timefold.solver.service.definition.api.domain.ConstraintReference;
                        import ai.timefold.solver.service.definition.api.domain.DataFormat;
                        import ai.timefold.solver.service.definition.api.domain.ModelRequest;

                        public class Test {
                                ModelInput input;
                                ModelOutput output;
                                ModelConvertor<Object, ModelInput, ModelOutput> convertor;
                                ModelRequest<ModelInput, ModelConfigOverrides> request;
                                ModelConstraintJustification justification;
                                ConstraintReference constraintReference;
                                ModelMaturityLevel maturityLevel = ModelMaturityLevel.STABLE;
                                DataFormat dataFormat = DataFormat.INTEGER;
                        }""",
                """
                        package timefold;

                        import ai.timefold.solver.service.definition.api.conversion.ModelConvertor;
                        import ai.timefold.solver.service.definition.api.description.ConstraintReference;
                        import ai.timefold.solver.service.definition.api.description.ModelMaturityLevel;
                        import ai.timefold.solver.service.definition.api.dto.dataset.ModelConfigOverrides;
                        import ai.timefold.solver.service.definition.api.dto.dataset.ModelInput;
                        import ai.timefold.solver.service.definition.api.dto.dataset.ModelOutput;
                        import ai.timefold.solver.service.definition.api.dto.dataset.ModelRequest;
                        import ai.timefold.solver.service.definition.api.dto.scoreanalysis.ModelConstraintJustification;
                        import ai.timefold.solver.service.definition.api.metrics.DataFormat;

                        public class Test {
                                ModelInput input;
                                ModelOutput output;
                                ModelConvertor<Object, ModelInput, ModelOutput> convertor;
                                ModelRequest<ModelInput, ModelConfigOverrides> request;
                                ModelConstraintJustification justification;
                                ConstraintReference constraintReference;
                                ModelMaturityLevel maturityLevel = ModelMaturityLevel.STABLE;
                                DataFormat dataFormat = DataFormat.INTEGER;
                        }"""));
    }

    @Test
    void migrateConfigurationTypes() {
        rewriteRun(java(
                """
                        package timefold;

                        import ai.timefold.solver.service.definition.api.SolvingStatus;
                        import ai.timefold.solver.service.definition.api.configuration.MapsConfiguration;
                        import ai.timefold.solver.service.definition.api.termination.SolverTerminationConfig;

                        public class Test {
                                MapsConfiguration mapsConfiguration;
                                SolverTerminationConfig terminationConfig;
                                SolvingStatus solvingStatus = SolvingStatus.SOLVING_ACTIVE;
                        }""",
                """
                        package timefold;

                        import ai.timefold.solver.service.definition.api.dto.dataset.MapsConfiguration;
                        import ai.timefold.solver.service.definition.api.dto.dataset.SolvingStatus;
                        import ai.timefold.solver.service.definition.api.dto.solve.SolverTerminationConfig;

                        public class Test {
                                MapsConfiguration mapsConfiguration;
                                SolverTerminationConfig terminationConfig;
                                SolvingStatus solvingStatus = SolvingStatus.SOLVING_ACTIVE;
                        }"""));
    }

    @Test
    void migrateValidationTypes() {
        rewriteRun(java(
                """
                        package timefold;

                        import ai.timefold.solver.service.definition.api.validation.AbstractIssue;
                        import ai.timefold.solver.service.definition.api.validation.IssueSeverity;
                        import ai.timefold.solver.service.definition.api.validation.ValidationBuilder;
                        import ai.timefold.solver.service.definition.api.validation.dto.ValidationResult;
                        import ai.timefold.solver.service.definition.api.validation.metadata.IssueMessage;

                        public class TestIssue extends AbstractIssue {
                                IssueSeverity severity = IssueSeverity.ERROR;
                                IssueMessage message;
                                ValidationBuilder builder;
                                ValidationResult result;
                        }""",
                """
                        package timefold;

                        import ai.timefold.solver.service.definition.api.dto.validation.ValidationResult;
                        import ai.timefold.solver.service.definition.api.dto.validation.issue.AbstractIssue;
                        import ai.timefold.solver.service.definition.api.dto.validation.issue.IssueSeverity;
                        import ai.timefold.solver.service.definition.api.dto.validation.issue.metadata.IssueMessage;
                        import ai.timefold.solver.service.definition.api.validation.model.ValidationBuilder;

                        public class TestIssue extends AbstractIssue {
                                IssueSeverity severity = IssueSeverity.ERROR;
                                IssueMessage message;
                                ValidationBuilder builder;
                                ValidationResult result;
                        }"""));
    }

    @Test
    void migrateDemoDataToPreviewApi() {
        rewriteRun(java(
                """
                        package timefold;

                        import ai.timefold.solver.service.definition.api.data.DemoData;
                        import ai.timefold.solver.service.definition.api.data.DemoDataGenerator;

                        public class Test {
                                DemoDataGenerator generator;
                                DemoData demoData;
                        }""",
                """
                        package timefold;

                        import ai.timefold.solver.service.definition.preview.api.demo.DemoData;
                        import ai.timefold.solver.service.definition.preview.api.demo.DemoDataGenerator;

                        public class Test {
                                DemoDataGenerator generator;
                                DemoData demoData;
                        }"""));
    }

    @Test
    void migrateInternalTypes() {
        rewriteRun(java(
                """
                        package timefold;

                        import ai.timefold.solver.service.definition.api.ResourceType;

                        public class Test {
                                ResourceType resourceType = new ResourceType("vehicle");
                        }""",
                """
                        package timefold;

                        import ai.timefold.solver.service.definition.internal.descriptor.ResourceType;

                        public class Test {
                                ResourceType resourceType = new ResourceType("vehicle");
                        }"""));
    }

}
