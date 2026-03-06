package ai.timefold.solver.migration.common;

import java.util.ArrayList;
import java.util.List;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;

public class RemoveGenericTypeFromMethodRecipe extends Recipe {

    private final MethodMatcher methodMatcher;
    private final int index;

    public RemoveGenericTypeFromMethodRecipe(String methodPattern, int index) {
        this.methodMatcher = new MethodMatcher(methodPattern, true);
        this.index = index;
    }

    @Override
    public String getDisplayName() {
        return "Remove one generic type from a method call";
    }

    @Override
    public String getDescription() {
        return "Removes a specific type from a from a method call.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                J.MethodInvocation mi = super.visitMethodInvocation(method, executionContext);
                if (mi.getTypeParameters() != null) {
                    List<Expression> typeParams = new ArrayList<>(mi.getTypeParameters());
                    if (methodMatcher.matches(mi) && index < typeParams.size()) {
                        typeParams.remove(index);
                        mi = mi.withTypeParameters(JContainer.withElements(JContainer.empty(), typeParams));
                    }
                }
                return mi;
            }
        };
    }

}
