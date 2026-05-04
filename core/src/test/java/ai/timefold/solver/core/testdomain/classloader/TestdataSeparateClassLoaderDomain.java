package ai.timefold.solver.core.testdomain.classloader;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.impl.domain.common.accessor.gizmo.GizmoClassLoader;

public class TestdataSeparateClassLoaderDomain {
    private static final GizmoClassLoader CLASS_LOADER = new GizmoClassLoader();
    private static boolean initialized = false;

    static {
        initialize();
    }

    private static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        var classBytecodeMap = TestdataDomainImplementor.generateClasses();
        for (var entry : classBytecodeMap.entrySet()) {
            CLASS_LOADER.storeBytecode(entry.getKey(), entry.getValue());
        }
    }

    public static ClassLoader getClassLoader() {
        return CLASS_LOADER;
    }

    public static Class<?> getTestdataEntityClass() {
        try {
            return CLASS_LOADER.loadClass("ai.timefold.solver.core.testdomain.TestdataEntity");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static Class<?> getTestdataValueClass() {
        try {
            return CLASS_LOADER.loadClass("ai.timefold.solver.core.testdomain.TestdataValue");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static Class<?> getTestdataSolutionClass() {
        try {
            return CLASS_LOADER.loadClass("ai.timefold.solver.core.testdomain.TestdataSolution");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object generateSolution() {
        var solutionClass = getTestdataSolutionClass();
        var entityClass = getTestdataEntityClass();
        var valueClass = getTestdataValueClass();
        try {
            var solution = solutionClass.getConstructor(String.class).newInstance("TestdataSolution");
            var entityList = new ArrayList<>();
            var valueList = new ArrayList<>();

            for (var i = 0; i < 3; i++) {
                var entity = entityClass.getConstructor(String.class).newInstance("Generated Entity %d".formatted(i));
                entityList.add(entity);
            }

            for (var i = 0; i < 3; i++) {
                var value = valueClass.getConstructor(String.class).newInstance("Generated Value %d".formatted(i));
                valueList.add(value);
            }

            solutionClass.getMethod("setEntityList", List.class).invoke(solution, entityList);
            solutionClass.getMethod("setValueList", List.class).invoke(solution, valueList);
            return solution;
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }
}
