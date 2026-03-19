package ai.timefold.solver.migration;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.openrewrite.Recipe;
import org.openrewrite.maven.ChangePropertyValue;

public final class ChangeVersionRecipe extends AbstractRecipe {

    final String version;

    public ChangeVersionRecipe() {
        try (var is = ChangeVersionRecipe.class.getResourceAsStream("rewrite-timefold-solver-version.properties")) {
            var props = new Properties();
            props.load(is);
            version = props.getProperty("version");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load rewrite-timefold-solver-version.properties.", e);
        }
    }

    @Override
    public String getName() {
        return "ai.timefold.solver.migration.ChangeVersion";
    }

    @Override
    public String getDisplayName() {
        return "Change the Timefold version";
    }

    @Override
    public String getDescription() {
        return "Replaces the version of Timefold";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new ChangePropertyValue("version.ai.timefold.solver", version, false, true),
                new ChangePropertyValue("version.timefold", version, false, true),
                new ChangePropertyValue("ai.timefold.solver.version", version, false, true),
                new ChangePropertyValue("timefold.version", version, false, true),
                new ChangePropertyValue("timefoldVersion", version, false, true));
    }

}
