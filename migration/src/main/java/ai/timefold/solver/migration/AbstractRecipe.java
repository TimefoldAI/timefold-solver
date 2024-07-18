package ai.timefold.solver.migration;

import org.openrewrite.Recipe;
import org.openrewrite.java.JavaParser;

public abstract class AbstractRecipe extends Recipe {

    public static final JavaParser.Builder<?, ?> JAVA_PARSER = JavaParser.fromJavaVersion()
            .classpath(JavaParser.runtimeClasspath());

}
