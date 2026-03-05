package ai.timefold.solver.core.impl.domain.specification.testdata;

import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 * Helper class in the same package as the package-private domain classes,
 * providing the necessary Lookup and test data.
 */
public class LookupTestHelper {

    /**
     * Returns a Lookup from this package that can access the package-private domain classes.
     */
    public static MethodHandles.Lookup lookup() {
        return MethodHandles.lookup();
    }

    public static Class<?> solutionClass() {
        return PackagePrivateSolution.class;
    }

    public static List<Class<?>> entityClassList() {
        return List.of(PackagePrivateEntity.class);
    }

    public static Object createUninitializedSolution() {
        var v1 = new PackagePrivateValue("v1");
        var v2 = new PackagePrivateValue("v2");
        var e1 = new PackagePrivateEntity("e1");
        var e2 = new PackagePrivateEntity("e2");
        return new PackagePrivateSolution(List.of(v1, v2), List.of(e1, e2));
    }
}
