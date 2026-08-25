package ai.timefold.solver.migration.v2;

import static org.openrewrite.properties.Assertions.properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

@Execution(ExecutionMode.CONCURRENT)
class ServiceConfigPropertiesRenameMigrationRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(new ServiceConfigPropertiesRenameMigrationRecipe());
    }

    @Test
    void renameExactAndAliasedProperties() {
        rewriteRun(properties(
                """
                        timefold.application.id=my-model
                        ai.timefold.model.max-thread-count=1
                        ai.timefold.tenant.store=memory
                        """,
                """
                        timefold.model.id=my-model
                        timefold.model.max-thread-count=1
                        timefold.tenant.store=memory
                        """,
                spec -> spec.path("application.properties")));
    }

    @Test
    void doesNotRenameModelApiVersion() {
        rewriteRun(properties(
                """
                        model.api.version=v1
                        """,
                spec -> spec.path("application.properties")));
    }

    @Test
    void renamePreservesLeadingQuarkusProfile() {
        rewriteRun(properties(
                """
                        %dev.ai.timefold.platform.termination.spent-limit=PT30S
                        %test.ai.timefold.platform.termination.unimproved-spent-limit=PT1S
                        %dev,test.ai.timefold.model.max-thread-count=1
                        """,
                """
                        %dev.timefold.model.termination.spent-limit=PT30S
                        %test.timefold.model.termination.unimproved-spent-limit=PT1S
                        %dev,test.timefold.model.max-thread-count=1
                        """,
                spec -> spec.path("application.properties")));
    }

    @Test
    void renamePrefixedProperties() {
        rewriteRun(properties(
                """
                        ai.timefold.model.default-config.max-thread-count=1
                        ai.timefold.platform.termination.spent-limit=PT10M
                        timefold.application.contact.email=
                        timefold.model.rest.some-setting=true
                        ai.timefold.storage.bucket=my-bucket
                        ai.timefold.platform.some-future-property=value
                        """,
                """
                        timefold.model.default-config.max-thread-count=1
                        timefold.model.termination.spent-limit=PT10M
                        timefold.model.contact.email=
                        timefold.rest.some-setting=true
                        timefold.storage.bucket=my-bucket
                        timefold.platform.some-future-property=value
                        """,
                spec -> spec.path("application.properties")));
    }

    @Test
    void removesSelfReferencingPropertyLeftBehindByRename() {
        rewriteRun(properties(
                """
                        timefold.application.id=my-model
                        %test.ai.timefold.platform.model=${timefold.application.id}
                        """,
                """
                        timefold.model.id=my-model
                        """,
                spec -> spec.path("application.properties")));
    }

    @Test
    void updatesReferencesToRenamedProperties() {
        rewriteRun(properties(
                """
                        timefold.application.id=my-model
                        timefold.application.version=v1
                        quarkus.rest.path=${timefold.application.version}
                        %container.quarkus.container-image.name=model-${timefold.application.id}-${timefold.application.version}
                        """,
                """
                        timefold.model.id=my-model
                        timefold.model.api-version=v1
                        quarkus.rest.path=${timefold.model.api-version}
                        %container.quarkus.container-image.name=model-${timefold.model.id}-${timefold.model.api-version}
                        """,
                spec -> spec.path("application.properties")));
    }

}
