package ai.timefold.solver.migration.common;

import java.util.regex.Pattern;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.tree.Properties;

/**
 * Removes property entries whose value is just a "${...}" reference back to the property's own key, e.g.
 * {@code %test.timefold.model.id=${timefold.model.id}}. Such entries can be left behind after a property (and its
 * references) were renamed to the same new name under a Quarkus profile.
 */
public final class RemoveSelfReferencingPropertyRecipe extends Recipe {

    // An optional leading Quarkus profile, e.g. "%dev." or "%dev,test.", is not part of the interpolated reference.
    private static final Pattern PROFILE_PREFIX = Pattern.compile("^%[^.]+\\.(.*)$");

    @Override
    public String getDisplayName() {
        return "Remove self-referencing properties";
    }

    @Override
    public String getDescription() {
        return "Removes property entries whose value is only a \"${...}\" reference back to the property's own key.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new PropertiesVisitor<>() {
            @Override
            public Properties visitEntry(Properties.Entry entry, ExecutionContext ctx) {
                Properties.Entry e = (Properties.Entry) super.visitEntry(entry, ctx);
                var matcher = PROFILE_PREFIX.matcher(e.getKey());
                String unprefixedKey = matcher.matches() ? matcher.group(1) : e.getKey();
                if (e.getValue().getText().equals("${" + unprefixedKey + "}")) {
                    return null;
                }
                return e;
            }
        };
    }
}
