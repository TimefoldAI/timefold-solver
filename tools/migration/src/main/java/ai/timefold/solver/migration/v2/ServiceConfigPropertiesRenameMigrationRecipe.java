package ai.timefold.solver.migration.v2;

import java.util.List;
import java.util.regex.Pattern;

import ai.timefold.solver.migration.AbstractRecipe;
import ai.timefold.solver.migration.common.RemoveSelfReferencingPropertyRecipe;
import ai.timefold.solver.migration.common.ReplacePropertyValueTextRecipe;

import org.openrewrite.Recipe;
import org.openrewrite.properties.ChangePropertyKey;

/**
 * Renames the Timefold Service configuration properties (typically found in {@code application.properties})
 * that were changed by the later unification of the configuration properties. Note that {@code model.api.version}
 * itself is intentionally left alone: it was already removed/renamed by an earlier, separate change and is no
 * longer part of this rename chain.
 * <p>
 * Every rename also matches (and preserves) an optional leading Quarkus profile, e.g.
 * {@code %dev.ai.timefold.platform.termination.spent-limit} becomes {@code %dev.timefold.model.termination.spent-limit}.
 */
public class ServiceConfigPropertiesRenameMigrationRecipe extends AbstractRecipe {

    // Optional leading Quarkus profile, e.g. "%dev." or "%dev,test.".
    private static final String PROFILE_PREFIX_REGEX = "(%[^.]+\\.)?";

    @Override
    public String getDisplayName() {
        return "Rename the unified Timefold Service configuration properties";
    }

    @Override
    public String getDescription() {
        return "Renames Timefold Service configuration properties to the names introduced by the unification of "
                + "the configuration properties.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                // Renames of a property that replaces one or two legacy names.
                exactRename("timefold.application.version", "timefold.model.api-version"),
                exactRename("ai.timefold.platform.model-version", "timefold.model.api-version"),

                exactRename("timefold.application.id", "timefold.model.id"),
                exactRename("ai.timefold.platform.model", "timefold.model.id"),

                exactRename("timefold.application.name", "timefold.model.name"),
                exactRename("timefold.application.description", "timefold.model.description"),
                exactRename("timefold.application.build-timestamp", "timefold.model.build-timestamp"),

                exactRename("ai.timefold.model.max-thread-count", "timefold.model.max-thread-count"),
                exactRename("ai.timefold.platform.model-resource", "timefold.model.rest-resource"),
                exactRename("ai.timefold.platform.models.validation.enable",
                        "timefold.model.schema.validation.enable"),
                exactRename("ai.timefold.platform.model.test.serialization.callback.disable",
                        "timefold.model.test.serialization.callback.disable"),

                exactRename("ai.timefold.tenant.store", "timefold.tenant.store"),
                exactRename("ai.timefold.maps.store", "timefold.maps.store"),
                exactRename("ai.timefold.models.store", "timefold.models.store"),
                exactRename("ai.timefold.plan.store", "timefold.plan.store"),

                // Prefix renames, most specific first; the generic "ai.timefold.platform." catch-all must run last,
                // otherwise it would shadow the more specific ai.timefold.platform.* renames above.
                prefixRename("ai.timefold.model.default-config.", "timefold.model.default-config."),
                prefixRename("ai.timefold.platform.termination.", "timefold.model.termination."),
                prefixRename("timefold.application.contact.", "timefold.model.contact."),
                prefixRename("timefold.rest.", "timefold.model.rest."),
                prefixRename("ai.timefold.storage.", "timefold.storage."),
                prefixRename("ai.timefold.platform.", "timefold.platform."),

                // The renames above only touch the property keys; also fix up "${...}" references to those keys
                // that appear in the values of other properties.
                new ReplacePropertyValueTextRecipe("${timefold.application.version}", "${model.api.version}"),
                new ReplacePropertyValueTextRecipe("${timefold.application.id}", "${timefold.model.id}"),
                new ReplacePropertyValueTextRecipe("${timefold.application.name}", "${timefold.model.name}"),
                new ReplacePropertyValueTextRecipe("${timefold.application.description}", "${timefold.model.description}"),
                new ReplacePropertyValueTextRecipe("${timefold.application.contact.email}", "${timefold.model.contact.email}"),
                new ReplacePropertyValueTextRecipe("${timefold.application.contact.name}", "${timefold.model.contact.name}"),
                new ReplacePropertyValueTextRecipe("${timefold.application.contact.url}", "${timefold.model.contact.url}"),

                // After the renames above, a profile override may now just point back at its own key, e.g.
                // "%test.timefold.model.id=${timefold.model.id}"; such dead entries can be dropped.
                new RemoveSelfReferencingPropertyRecipe());
    }

    // Matches the whole key, keeping an optional leading Quarkus profile (group 1) intact.
    private static ChangePropertyKey exactRename(String oldKey, String newKey) {
        String pattern = "^" + PROFILE_PREFIX_REGEX + Pattern.quote(oldKey) + "$";
        return new ChangePropertyKey(pattern, "$1" + newKey, null, true);
    }

    // Matches a key prefix, keeping an optional leading Quarkus profile (group 1) and the key suffix (group 2) intact.
    private static ChangePropertyKey prefixRename(String oldPrefix, String newPrefix) {
        String pattern = "^" + PROFILE_PREFIX_REGEX + Pattern.quote(oldPrefix) + "(.*)$";
        return new ChangePropertyKey(pattern, "$1" + newPrefix + "$2", null, true);
    }
}
