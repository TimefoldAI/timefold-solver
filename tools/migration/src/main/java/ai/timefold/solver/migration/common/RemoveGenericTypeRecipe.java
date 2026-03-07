package ai.timefold.solver.migration.common;

import java.util.ArrayList;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

public final class RemoveGenericTypeRecipe extends Recipe {

    private final String clazz;
    private final int index;

    public RemoveGenericTypeRecipe(String clazz, int index) {
        this.clazz = clazz;
        this.index = index;
    }

    @Override
    public String getDisplayName() {
        return "Remove one generic type";
    }

    @Override
    public String getDescription() {
        return "Removes a generic type from a multi-type generic declaration.";
    }

    @Override
    public JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.ParameterizedType visitParameterizedType(J.ParameterizedType parameterizedType, ExecutionContext ctx) {
                J.ParameterizedType pt = super.visitParameterizedType(parameterizedType, ctx);

                if (TypeUtils.isOfClassType(pt.getType(), clazz)) {
                    var typeParams = pt.getTypeParameters();
                    if (typeParams != null && typeParams.size() > index) {
                        var newTypeParams = new ArrayList<>(typeParams);
                        newTypeParams.remove(index);
                        return pt.withTypeParameters(newTypeParams);
                    }
                }
                return pt;
            }
        };
    }
}
