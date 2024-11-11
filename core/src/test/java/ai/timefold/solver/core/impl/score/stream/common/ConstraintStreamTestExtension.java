package ai.timefold.solver.core.impl.score.stream.common;

import java.util.List;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.score.stream.ConstraintStream;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;

import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

/**
 * This extension helps implement parameterized {@link ConstraintStream} tests.
 * It provides invocation contexts representing test matrix with {@link ConstraintMatchPolicy} axis.
 * <p>
 * Each invocation context includes two additional extensions being {@link ParameterResolver parameter resolvers} that
 * populate the test class constructor with the test data. Since each CS test class has dozens of test methods
 * this is a more practical approach than using {@code @ParameterizedTest} where test data are consumed through method
 * parameters.
 */
public class ConstraintStreamTestExtension implements TestTemplateInvocationContextProvider {

    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        return true;
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
        return Stream
                .of(ConstraintMatchPolicy.ENABLED, ConstraintMatchPolicy.ENABLED_WITHOUT_JUSTIFICATIONS,
                        ConstraintMatchPolicy.DISABLED)
                .map(ConstraintStreamTestExtension::invocationContext);
    }

    private static TestTemplateInvocationContext invocationContext(ConstraintMatchPolicy constraintMatchPolicy) {
        return new TestTemplateInvocationContext() {

            @Override
            public String getDisplayName(int invocationIndex) {
                return "constraintMatchPolicy=" + constraintMatchPolicy;
            }

            @Override
            public List<Extension> getAdditionalExtensions() {
                return List.of(parameterResolver(ConstraintMatchPolicy.class, constraintMatchPolicy));
            }
        };
    }

    private static <T> ParameterResolver parameterResolver(Class<T> type, T value) {
        return new ParameterResolver() {
            @Override
            public boolean supportsParameter(
                    ParameterContext parameterContext,
                    ExtensionContext extensionContext) throws ParameterResolutionException {
                return parameterContext.getParameter().getType().equals(type);
            }

            @Override
            public Object resolveParameter(
                    ParameterContext parameterContext,
                    ExtensionContext extensionContext) throws ParameterResolutionException {
                return value;
            }
        };
    }
}
