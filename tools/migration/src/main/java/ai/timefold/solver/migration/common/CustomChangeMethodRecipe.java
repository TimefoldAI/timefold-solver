package ai.timefold.solver.migration.common;

import java.util.List;
import java.util.stream.Stream;

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

public final class CustomChangeMethodRecipe extends AbstractRecipe {

    private final MatcherMeta matcherMeta;

    public CustomChangeMethodRecipe(String clazz, String sourceMethodPattern, String newMethodPattern) {
        this.matcherMeta = new MatcherMeta(clazz, sourceMethodPattern, newMethodPattern);
    }

    @Override
    public String getDisplayName() {
        return "SolverManager: use builder API";
    }

    @Override
    public String getDescription() {
        return "Use `solveBuilder()` instead of deprecated solve methods on `SolveManager`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.or(new UsesMethod<>(matcherMeta.methodMatcher)),
                new JavaIsoVisitor<>() {

                    @Override
                    public Expression visitExpression(Expression expression, ExecutionContext ctx) {
                        final Expression e = super.visitExpression(expression, ctx);
                        if (!matcherMeta.methodMatcher.matches(e)) {
                            return e;
                        }
                        J.MethodInvocation mi = (J.MethodInvocation) e;
                        Expression select = mi.getSelect();
                        List<Expression> arguments = mi.getArguments();

                        String pattern = "#{any(" + matcherMeta.clazz + ")}" + matcherMeta.pattern;
                        if (arguments.isEmpty() || (arguments.size() == 1 && arguments.get(0) instanceof J.Empty)) {
                            return JavaTemplate.builder(pattern)
                                    .javaParser(JAVA_PARSER)
                                    .build()
                                    .apply(getCursor(), e.getCoordinates().replace(), select);
                        } else {
                            return JavaTemplate.builder(pattern)
                                    .javaParser(JAVA_PARSER)
                                    .build()
                                    .apply(getCursor(), e.getCoordinates().replace(),
                                            Stream.concat(Stream.of(select), arguments.stream()).toArray());
                        }

                    }
                });
    }

    private static final class MatcherMeta {

        private final String clazz;
        public final MethodMatcher methodMatcher;
        public final String methodName;
        private final String pattern;

        public MatcherMeta(String clazz, String method, String pattern) {
            this.clazz = clazz;
            this.methodMatcher = new MethodMatcher(clazz + " " + method);
            this.methodName = method;
            this.pattern = pattern;
        }
    }

}
