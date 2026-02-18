package ai.timefold.solver.spring.boot.autoconfigure;

import java.util.Map;

import ai.timefold.solver.core.config.solver.SolverConfig;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;

public class TimefoldSolverAotContribution implements BeanFactoryInitializationAotContribution {
    private final Map<String, SolverConfig> solverConfigMap;

    public TimefoldSolverAotContribution(Map<String, SolverConfig> solverConfigMap) {
        this.solverConfigMap = solverConfigMap;
    }

    /**
     * Register a type for reflection, allowing introspection
     * of its members at runtime in a native build.
     */
    private static void registerType(ReflectionHints reflectionHints, Class<?> type) {
        reflectionHints.registerType(type,
                MemberCategory.ACCESS_PUBLIC_FIELDS,
                MemberCategory.ACCESS_DECLARED_FIELDS,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_METHODS,
                MemberCategory.INVOKE_PUBLIC_METHODS);
    }

    @Override
    public void applyTo(GenerationContext generationContext, BeanFactoryInitializationCode beanFactoryInitializationCode) {
        ReflectionHints reflectionHints = generationContext.getRuntimeHints().reflection();
        for (SolverConfig solverConfig : solverConfigMap.values()) {
            solverConfig.visitReferencedClasses(type -> {
                if (type != null) {
                    registerType(reflectionHints, type);
                }
            });
        }
    }
}
