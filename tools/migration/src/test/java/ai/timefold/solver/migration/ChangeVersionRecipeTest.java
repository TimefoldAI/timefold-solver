package ai.timefold.solver.migration;

import static org.openrewrite.maven.Assertions.pomXml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

@Execution(ExecutionMode.CONCURRENT)
class ChangeVersionRecipeTest implements RewriteTest {

    private final ChangeVersionRecipe recipe = new ChangeVersionRecipe();

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(recipe);
    }

    @Test
    void versionAiTimefoldSolver() {
        rewriteRun(pomXml(
                """
                        <project>
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>com.example</groupId>
                          <artifactId>example</artifactId>
                          <version>1.0</version>
                          <properties>
                            <version.ai.timefold.solver>1.0.0</version.ai.timefold.solver>
                          </properties>
                        </project>""",
                """
                        <project>
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>com.example</groupId>
                          <artifactId>example</artifactId>
                          <version>1.0</version>
                          <properties>
                            <version.ai.timefold.solver>%s</version.ai.timefold.solver>
                          </properties>
                        </project>""".formatted(recipe.version)));
    }

    @Test
    void versionTimefold() {
        rewriteRun(pomXml(
                """
                        <project>
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>com.example</groupId>
                          <artifactId>example</artifactId>
                          <version>1.0</version>
                          <properties>
                            <version.timefold>1.0.0</version.timefold>
                          </properties>
                        </project>""",
                """
                        <project>
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>com.example</groupId>
                          <artifactId>example</artifactId>
                          <version>1.0</version>
                          <properties>
                            <version.timefold>%s</version.timefold>
                          </properties>
                        </project>""".formatted(recipe.version)));
    }

    @Test
    void timefoldVersionCamelCase() {
        rewriteRun(pomXml(
                """
                        <project>
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>com.example</groupId>
                          <artifactId>example</artifactId>
                          <version>1.0</version>
                          <properties>
                            <timefoldVersion>1.0.0</timefoldVersion>
                          </properties>
                        </project>""",
                """
                        <project>
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>com.example</groupId>
                          <artifactId>example</artifactId>
                          <version>1.0</version>
                          <properties>
                            <timefoldVersion>%s</timefoldVersion>
                          </properties>
                        </project>""".formatted(recipe.version)));
    }

    @Test
    void missingPropertyNotAdded() {
        rewriteRun(pomXml(
                """
                        <project>
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>com.example</groupId>
                          <artifactId>example</artifactId>
                          <version>1.0</version>
                        </project>"""));
    }

    @Test
    void unrelatedPropertyUntouched() {
        rewriteRun(pomXml(
                """
                        <project>
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>com.example</groupId>
                          <artifactId>example</artifactId>
                          <version>1.0</version>
                          <properties>
                            <some.other.version>1.0.0</some.other.version>
                          </properties>
                        </project>"""));
    }

}
