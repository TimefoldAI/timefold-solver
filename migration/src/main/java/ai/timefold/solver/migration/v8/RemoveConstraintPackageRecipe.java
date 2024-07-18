package ai.timefold.solver.migration.v8;

import java.util.List;

import ai.timefold.solver.migration.AbstractRecipe;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

public final class RemoveConstraintPackageRecipe extends AbstractRecipe {

    @Override
    public String getDisplayName() {
        return "Constraint Streams: don't use package name in the asConstraint() method";
    }

    @Override
    public String getDescription() {
        return "Remove the use of constraint package from `asConstraint(package, name)`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.or(new UsesMethod<>(new MethodMatcher(
                        "ai.timefold.solver.core.api.score.stream.ConstraintBuilder asConstraint(String, String)"))),
                new JavaIsoVisitor<>() {

                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        Expression select = method.getSelect();
                        List<Expression> arguments = method.getArguments();

                        String templateCode = "#{any(ai.timefold.solver.core.api.score.stream.ConstraintBuilder)}\n" +
                                ".asConstraint(\"#{}\")";
                        JavaTemplate template = JavaTemplate.builder(templateCode)
                                .javaParser(JAVA_PARSER)
                                .build();
                        return template.apply(getCursor(),
                                method.getCoordinates().replace(), select,
                                mergeExpressions(arguments.get(0), arguments.get(1)));
                    }
                });
    }

    private String mergeExpressions(Expression constraintPackage, Expression constraintName) {
        return constraintPackage.toString() + "." + constraintName.toString();
    }

}
