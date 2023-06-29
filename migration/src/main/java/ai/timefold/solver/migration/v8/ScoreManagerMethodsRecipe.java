package ai.timefold.solver.migration.v8;

import java.util.Arrays;
import java.util.List;

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

public class ScoreManagerMethodsRecipe extends Recipe {

    private static final MatcherMeta[] MATCHER_METAS = {
            new MatcherMeta("getSummary(..)"),
            new MatcherMeta("explainScore(..)"),
            new MatcherMeta("updateScore(..)")
    };

    @Override
    public String getDisplayName() {
        return "ScoreManager: explain(), update()";
    }

    @Override
    public String getDescription() {
        return "Use `explain()` and `update()` " +
                "   instead of `explainScore()`, `updateScore()` and `getSummary()`.";
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
                        List<Expression> arguments = mi.getArguments();
                        String pattern = "#{any(" + matcherMeta.classFqn + ")}." +
                                (matcherMeta.methodName.contains("Summary")
                                        ? "explain(#{any()}, SolutionUpdatePolicy.UPDATE_SCORE_ONLY).getSummary()"
                                        : matcherMeta.methodName.replace(")", ", SolutionUpdatePolicy.UPDATE_SCORE_ONLY)")
                                                .replace("..", "#{any()}")
                                                .replace("Score(", "("));
                        maybeAddImport("ai.timefold.solver.core.api.solver.SolutionUpdatePolicy");
                        return JavaTemplate.builder(pattern)
                                .javaParser(buildJavaParser())
                                .imports("ai.timefold.solver.core.api.solver.SolutionUpdatePolicy")
                                .build()
                                .apply(getCursor(), e.getCoordinates().replace(), select, arguments.get(0));
                    }
                });
    }

    public static JavaParser.Builder buildJavaParser() {
        return JavaParser.fromJavaVersion()
                .classpath(JavaParser.runtimeClasspath());
    }

    private static final class MatcherMeta {

        public final String classFqn;
        public final MethodMatcher methodMatcher;
        public final String methodName;

        public MatcherMeta(String method) {
            this.classFqn = "ai.timefold.solver.core.api.score.ScoreManager";
            this.methodMatcher = new MethodMatcher(classFqn + " " + method);
            this.methodName = method;
        }
    }

}
