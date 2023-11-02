package ai.timefold.solver.migration.v8;

import java.util.Arrays;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

public final class ConstraintRefRecipe extends Recipe {

    private static final MatcherMeta[] MATCHER_METAS = {
            new MatcherMeta("Constraint", "getConstraintId()", "constraintId()"),
            new MatcherMeta("Constraint", "getConstraintName()", "constraintName()"),
            new MatcherMeta("Constraint", "getConstraintPackage()", "packageName()"),

            new MatcherMeta("ConstraintMatch", "getConstraintId()", "constraintId()"),
            new MatcherMeta("ConstraintMatch", "getConstraintName()", "constraintName()"),
            new MatcherMeta("ConstraintMatch", "getConstraintPackage()", "packageName()"),

            new MatcherMeta("ConstraintMatchTotal", "getConstraintId()", "constraintId()"),
            new MatcherMeta("ConstraintMatchTotal", "getConstraintName()", "constraintName()"),
            new MatcherMeta("ConstraintMatchTotal", "getConstraintPackage()", "packageName()"),
    };

    @Override
    public String getDisplayName() {
        return "Replace getConstraint*() with getConstraintRef()";
    }

    @Override
    public String getDescription() {
        return "Use `getConstraintRef()` instead of `getConstraintId()` et al.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext>[] visitors = Arrays.stream(MATCHER_METAS)
                .map(m -> new UsesMethod<>(m.methodMatcher))
                .toArray(TreeVisitor[]::new);
        return Preconditions.check(
                Preconditions.or(visitors),
                new JavaIsoVisitor<>() {

                    @Override
                    public Expression visitExpression(Expression expression, ExecutionContext executionContext) {
                        final Expression e = super.visitExpression(expression, executionContext);

                        MatcherMeta matcherMeta = Arrays.stream(MATCHER_METAS).filter(m -> m.methodMatcher.matches(e))
                                .findFirst().orElse(null);
                        if (matcherMeta == null) {
                            return e;
                        }
                        J.MethodInvocation mi = (J.MethodInvocation) e;
                        Expression select = mi.getSelect();

                        String clz = "#{any(" + matcherMeta.scoreClassFqn + ")}";
                        String pattern = clz + ".getConstraintRef()." + matcherMeta.newMethodName;
                        maybeAddImport("ai.timefold.solver.core.api.score.constraint.ConstraintRef");
                        return JavaTemplate.builder(pattern)
                                .javaParser(buildJavaParser())
                                .build()
                                .apply(getCursor(), e.getCoordinates().replace(), select);
                    }
                });
    }

    public static JavaParser.Builder buildJavaParser() {
        return JavaParser.fromJavaVersion()
                .classpath(JavaParser.runtimeClasspath());
    }

    private static final class MatcherMeta {

        public final String scoreClassFqn;
        public final MethodMatcher methodMatcher;
        public final String methodName;
        public final String newMethodName;

        public MatcherMeta(String select, String method, String newMethod) {
            String className = switch (select) {
                case "ConstraintMatch", "ConstraintMatchTotal" ->
                    "ai.timefold.solver.core.api.score.constraint." + select;
                case "Constraint" -> "ai.timefold.solver.core.api.score.stream." + select;
                default -> throw new IllegalArgumentException("Unexpected value: " + select);
            };
            this.scoreClassFqn = className;
            this.methodMatcher = new MethodMatcher(scoreClassFqn + " " + method);
            this.methodName = method;
            this.newMethodName = newMethod;
        }
    }

}
