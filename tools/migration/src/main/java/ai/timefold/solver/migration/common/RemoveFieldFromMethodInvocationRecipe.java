package ai.timefold.solver.migration.common;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

public class RemoveFieldFromMethodInvocationRecipe extends Recipe {

    private final MethodMatcher methodMatcher;

    public RemoveFieldFromMethodInvocationRecipe(String methodPattern) {
        this.methodMatcher = new MethodMatcher(methodPattern);
    }

    @Override
    public String getDisplayName() {
        return "Remove fields from method invocation";
    }

    @Override
    public String getDescription() {
        return "Removes field declarations based on the method invocation.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<>() {

            @Override
            public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext executionContext) {
                J.VariableDeclarations v =
                        (J.VariableDeclarations) super.visitVariableDeclarations(multiVariable, executionContext);
                for (J.VariableDeclarations.NamedVariable nv : v.getVariables()) {
                    if (nv.getInitializer() instanceof J.MethodInvocation mi && methodMatcher.matches(mi)) {
                        return null;
                    }
                }
                return v;
            }
        };
    }
}
