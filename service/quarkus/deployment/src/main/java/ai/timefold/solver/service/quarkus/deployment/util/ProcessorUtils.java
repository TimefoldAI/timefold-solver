package ai.timefold.solver.service.quarkus.deployment.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

public class ProcessorUtils {

    public static final String DEFAULT_TYPES_PACKAGE = "ai.timefold.solver.service.quarkus.deployment.defaults";

    /**
     * Finds a single direct subclass of a parent class excluding given subclasses.
     *
     * @param index {@link IndexView} to search in
     * @param parentClass parent class to look for a subclass of
     * @param excludedSubclasses subclasses excluded from the search
     */
    public static Optional<ClassInfo> getDirectSubclassExcluding(IndexView index, DotName parentClass,
            DotName... excludedSubclasses) {
        Predicate<ClassInfo> exclusionPredicate = (ClassInfo subclass) -> {
            for (DotName excludedSubclass : excludedSubclasses) {
                if (excludedSubclass.equals(subclass.name())) {
                    return false;
                }
            }
            return true;
        };
        List<ClassInfo> filteredDirectSubclasses = index.getKnownDirectSubclasses(parentClass)
                .stream()
                .filter(exclusionPredicate)
                .toList();
        if (filteredDirectSubclasses.isEmpty()) {
            return Optional.empty();
        }

        if (filteredDirectSubclasses.size() > 1) {
            throw new IllegalStateException(
                    "Expected exactly one direct subtype of '" + parentClass
                            + "' but found %d (%s)".formatted(filteredDirectSubclasses.size(),
                                    filteredDirectSubclasses.stream().map(ClassInfo::name).map(DotName::toString)
                                            .collect(Collectors.joining(", "))));
        }

        return Optional.of(filteredDirectSubclasses.getFirst());
    }

    /**
     * Finds the first direct sub-interface of the given type or the first direct implementor if no sub-interface is found.
     */
    public static ClassInfo getFirstDirectSubInterfaceOrImplementorOf(DotName type, IndexView index) {
        return getFirstDirectSubInterfaceOf(type, index)
                .or(() -> getFirstDirectImplementorOf(type, index))
                .orElseThrow(() -> new IllegalStateException("Unable to find sub-interface or implementor of "
                        + type.toString()));
    }

    private static Optional<ClassInfo> getFirstDirectImplementorOf(DotName type, IndexView index) {
        return getType(type, index::getKnownDirectImplementors);
    }

    public static Optional<ClassInfo> getFirstDirectSubInterfaceOf(DotName type, IndexView index) {
        return getType(type, index::getKnownDirectSubinterfaces);
    }

    private static Optional<ClassInfo> getType(DotName type, Function<DotName, Collection<ClassInfo>> typeFinder) {
        Function<Collection<ClassInfo>, Optional<ClassInfo>> typeSelector = (Collection<ClassInfo> types) -> {
            if (types.size() == 1) {
                return types.stream().findFirst();
            } else {
                throw new IllegalStateException(
                        "Expected exactly one type of '" + type + "' but found %d (%s)".formatted(types.size(),
                                types.stream().map(ClassInfo::name).map(DotName::toString).collect(Collectors.joining(", "))));
            }
        };

        return findType(type, typeFinder, typeSelector);
    }

    private static Optional<ClassInfo> findType(DotName type, Function<DotName, Collection<ClassInfo>> typeFinder,
            Function<Collection<ClassInfo>, Optional<ClassInfo>> typeSelector) {
        Collection<ClassInfo> types = typeFinder.apply(type);

        if (types.isEmpty()) {
            return Optional.empty();
        } else if (types.size() == 1) {
            return Optional.of(types.iterator().next());
        } else {
            var nonDefaultTypes = types.stream()
                    .filter(found -> !found.name().packagePrefix().equals(DEFAULT_TYPES_PACKAGE))
                    .toList();
            return typeSelector.apply(nonDefaultTypes);
        }
    }

    public static Optional<ClassInfo> findType(DotName type, Function<DotName, Collection<ClassInfo>> typeFinder) {
        Function<Collection<ClassInfo>, Optional<ClassInfo>> typeSelector =
                (Collection<ClassInfo> types) -> types.stream().findFirst();

        return findType(type, typeFinder, typeSelector);
    }

    public static Optional<ClassInfo> findLastInHierarchyType(DotName type,
            Function<DotName, Collection<ClassInfo>> typeFinder) {
        Function<Collection<ClassInfo>, Optional<ClassInfo>> typeSelector =
                (Collection<ClassInfo> types) -> {
                    // Map to track subclass relationships (parent -> set of children)
                    Map<DotName, Set<DotName>> subclassMap = new HashMap<>();
                    Set<DotName> allSubclasses = new HashSet<>();

                    // Build the subclass relationship map
                    for (ClassInfo classInfo : types) {
                        DotName superName = classInfo.superName();
                        DotName className = classInfo.name();

                        if (superName.local().equals(Object.class.getName())) {
                            continue;
                        }
                        subclassMap.computeIfAbsent(superName, k -> new HashSet<>()).add(className);
                        allSubclasses.add(className);
                    }

                    // Find the leaf classes (classes that are not parents of any other class)
                    Set<DotName> leafClasses = new HashSet<>(allSubclasses);
                    leafClasses.removeAll(subclassMap.keySet()); // Remove classes that are parents
                    DotName firstEndOfHierarchyType = leafClasses.iterator().next();
                    return types.stream().filter(cl -> cl.name().equals(firstEndOfHierarchyType)).findFirst();
                };

        return findType(type, typeFinder, typeSelector);
    }

    public static ClassInfo findRequiredLastInHierarchyType(DotName type, Function<DotName, Collection<ClassInfo>> typeFinder) {
        return findLastInHierarchyType(type, typeFinder)
                .orElseThrow(() -> new IllegalStateException("Unable to find implementor of " + type.toString()));
    }

    public static Function<DotName, Collection<ClassInfo>> excludeType(Function<DotName, Collection<ClassInfo>> typeFinder,
            DotName excludedType) {
        return typeFinder.andThen((classInfos -> excludeType(classInfos, excludedType)));
    }

    public static Collection<ClassInfo> excludeType(Collection<ClassInfo> classInfos, DotName excludedType) {
        return classInfos.stream().filter(classInfo -> !classInfo.name().equals(excludedType)).toList();
    }

    public static Optional<ClassInfo> findFirstImplementorOf(DotName type, IndexView index) {
        return findType(type, index::getAllKnownImplementations);
    }

    public static ClassInfo getFirstImplementorOf(DotName type, IndexView index) {
        return findType(type, index::getAllKnownImplementations)
                .orElseThrow(() -> new IllegalStateException("Unable to find implementor of " + type.toString()));
    }

    public static ClassInfo findLastInHierarchyType(DotName type, IndexView index) {
        return findLastInHierarchyType(type, index::getAllKnownImplementations)
                .orElseThrow(() -> new IllegalStateException("Unable to find implementor of " + type.toString()));
    }

    public static void requireDefaultAndParameterizedConstructor(ClassInfo classInfo) {
        List<MethodInfo> constructors = classInfo.constructors();
        if (constructors.size() > 2) {
            throw new IllegalStateException("The " + classInfo.name() + " class must have at most two constructors: "
                    + "a mandatory one without any parameters for recording, "
                    + "an optional one with parameters for injection.");
        }
        if (constructors.getFirst().parametersCount() != 0 &&
                constructors.getLast().parametersCount() != 0) {
            throw new IllegalStateException(
                    "The " + classInfo.name() + " class is missing the mandatory constructor with no parameters.");
        }
    }

    public static void createDefaultConstructor(ClassCreator beanCreator, String generatedName) {
        MethodCreator constructor = beanCreator.getMethodCreator(MethodDescriptor.ofConstructor(generatedName));
        ResultHandle thisObj = constructor.getThis();

        ResultHandle[] params = new ResultHandle[0];

        constructor.invokeSpecialMethod(
                MethodDescriptor.ofConstructor(beanCreator.getSuperClass()),
                thisObj,
                params);

        constructor.returnValue(thisObj);
        constructor.close();
    }
}
