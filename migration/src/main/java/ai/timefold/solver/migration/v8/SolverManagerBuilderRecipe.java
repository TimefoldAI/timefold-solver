package ai.timefold.solver.migration.v8;

import java.util.Arrays;
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

public final class SolverManagerBuilderRecipe extends AbstractRecipe {

    private static final MatcherMeta[] MATCHER_METAS = {
            new MatcherMeta(
                    "solve(*..*,java.util.function.Function,java.util.function.Consumer,java.util.function.BiConsumer)",
                    ".solveBuilder().withProblemId(#{any()}).withProblemFinder(#{any()}).withFinalBestSolutionConsumer(#{any(java.util.function.Consumer)}).withExceptionHandler(#{any(java.util.function.BiConsumer)}).run()"),
            new MatcherMeta("solve(*..*,*..*,java.util.function.Consumer,java.util.function.BiConsumer)",
                    ".solveBuilder().withProblemId(#{any()}).withProblem(#{any()}).withFinalBestSolutionConsumer(#{any(java.util.function.Consumer)}).withExceptionHandler(#{any(java.util.function.BiConsumer)}).run()"),
            new MatcherMeta("solve(*..*,java.util.function.Function,java.util.function.Consumer)",
                    ".solveBuilder().withProblemId(#{any()}).withProblemFinder(#{any()}).withFinalBestSolutionConsumer(#{any(java.util.function.Consumer)}).run()"),
            new MatcherMeta("solveAndListen(*..*,java.util.function.Function,java.util.function.Consumer)",
                    ".solveBuilder().withProblemId(#{any()}).withProblemFinder(#{any()}).withBestSolutionConsumer(#{any(java.util.function.Consumer)}).run()"),
            new MatcherMeta(
                    "solveAndListen(*..*,java.util.function.Function,java.util.function.Consumer,java.util.function.BiConsumer)",
                    ".solveBuilder().withProblemId(#{any()}).withProblemFinder(#{any()}).withBestSolutionConsumer(#{any(java.util.function.Consumer)}).withExceptionHandler(#{any(java.util.function.BiConsumer)}).run()"),
            new MatcherMeta(
                    "solveAndListen(*..*,java.util.function.Function,java.util.function.Consumer,java.util.function.Consumer,java.util.function.BiConsumer)",
                    ".solveBuilder().withProblemId(#{any()}).withProblemFinder(#{any()}).withBestSolutionConsumer(#{any(java.util.function.Consumer)}).withFinalBestSolutionConsumer(#{any(java.util.function.Consumer)}).withExceptionHandler(#{any(java.util.function.BiConsumer)}).run()"),
    };

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

                        String pattern = "#{any(" + matcherMeta.classFqn + ")}" + matcherMeta.pattern;

                        return JavaTemplate.builder(pattern)
                                .javaParser(JAVA_PARSER)
                                .build()
                                .apply(getCursor(), e.getCoordinates().replace(),
                                        Stream.concat(Stream.of(select), arguments.stream()).toArray());
                    }
                });
    }

    private static final class MatcherMeta {

        private final String classFqn;
        public final MethodMatcher methodMatcher;
        public final String methodName;
        private final String pattern;

        public MatcherMeta(String method, String pattern) {
            this.classFqn = "ai.timefold.solver.core.api.solver.SolverManager";
            this.methodMatcher = new MethodMatcher(classFqn + " " + method);
            this.methodName = method;
            this.pattern = pattern;
        }
    }

}
