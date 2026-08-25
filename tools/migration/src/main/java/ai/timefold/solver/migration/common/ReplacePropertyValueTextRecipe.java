package ai.timefold.solver.migration.common;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.tree.Properties;

/**
 * Replaces every literal occurrence of {@code find} with {@code replace} in property values, e.g. to fix up
 * {@code ${some.renamed.property}} references after the property itself was renamed.
 */
public final class ReplacePropertyValueTextRecipe extends Recipe {

    private final String find;
    private final String replace;

    public ReplacePropertyValueTextRecipe(String find, String replace) {
        this.find = find;
        this.replace = replace;
    }

    @Override
    public String getDisplayName() {
        return "Replace text in property values";
    }

    @Override
    public String getDescription() {
        return "Replaces every literal occurrence of a string with another string in property values.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new PropertiesVisitor<>() {
            @Override
            public Properties visitEntry(Properties.Entry entry, ExecutionContext ctx) {
                Properties.Entry e = (Properties.Entry) super.visitEntry(entry, ctx);
                String text = e.getValue().getText();
                if (text.contains(find)) {
                    e = e.withValue(e.getValue().withText(text.replace(find, replace)));
                }
                return e;
            }
        };
    }
}
