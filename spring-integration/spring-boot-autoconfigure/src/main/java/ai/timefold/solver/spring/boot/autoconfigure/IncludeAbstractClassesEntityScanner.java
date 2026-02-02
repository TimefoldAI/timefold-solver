package ai.timefold.solver.spring.boot.autoconfigure;

import static ai.timefold.solver.spring.boot.autoconfigure.util.LambdaUtils.rethrowFunction;
import static java.util.Collections.emptyList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.persistence.autoconfigure.EntityScanPackages;
import org.springframework.boot.persistence.autoconfigure.EntityScanner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;

public class IncludeAbstractClassesEntityScanner extends EntityScanner {

    private final ApplicationContext context;

    public IncludeAbstractClassesEntityScanner(ApplicationContext context) {
        super(context);
        this.context = context;
    }

    public <T> Class<? extends T> findFirstImplementingClass(Class<T> targetClass) {
        List<Class<? extends T>> classes = findImplementingClassList(targetClass);
        if (!classes.isEmpty()) {
            return classes.get(0);
        }
        return null;
    }

    private Set<String> findPackages() {
        Set<String> packages = new HashSet<>();
        packages.addAll(AutoConfigurationPackages.get(context));
        EntityScanPackages entityScanPackages = EntityScanPackages.get(context);
        packages.addAll(entityScanPackages.getPackageNames());
        return packages;
    }

    public <T> List<Class<? extends T>> findImplementingClassList(Class<T> targetClass) {
        if (!AutoConfigurationPackages.has(context)) {
            return emptyList();
        }
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.setEnvironment(context.getEnvironment());
        scanner.setResourceLoader(context);
        scanner.addIncludeFilter(new AssignableTypeFilter(targetClass));
        Set<String> packages = findPackages();
        return packages.stream()
                .flatMap(basePackage -> scanner.findCandidateComponents(basePackage).stream())
                // findCandidateComponents can return the same package for different base packages
                .distinct()
                .sorted(Comparator.comparing(BeanDefinition::getBeanClassName))
                .map(candidate -> {
                    try {
                        return (Class<? extends T>) ClassUtils.forName(candidate.getBeanClassName(), context.getClassLoader())
                                .asSubclass(targetClass);
                    } catch (ClassNotFoundException e) {
                        throw new IllegalStateException("The %s class (%s) cannot be found."
                                .formatted(targetClass.getSimpleName(), candidate.getBeanClassName()), e);
                    }
                })
                .collect(Collectors.toList());
    }

    @SafeVarargs
    public final List<Class<?>> findClassesWithAnnotation(Class<? extends Annotation>... annotations) {
        if (!AutoConfigurationPackages.has(context)) {
            return emptyList();
        }
        Set<String> packages = findPackages();
        return packages.stream().flatMap(rethrowFunction(
                basePackage -> findAllClassesUsingClassLoader(this.context.getClassLoader(), basePackage).stream()))
                .filter(clazz -> hasAnyFieldOrMethodWithAnnotation(clazz, annotations))
                .toList();
    }

    private boolean hasAnyFieldOrMethodWithAnnotation(Class<?> clazz, Class<? extends Annotation>[] annotations) {
        List<Field> fieldList = List.of(clazz.getDeclaredFields());
        List<Method> methodList = List.of(clazz.getDeclaredMethods());
        return List.of(annotations).stream().anyMatch(a -> fieldList.stream().anyMatch(f -> f.getAnnotation(a) != null)
                || methodList.stream().anyMatch(m -> m.getDeclaredAnnotation(a) != null));
    }

    public boolean hasSolutionOrEntityClasses() {
        try {
            return !scan(PlanningSolution.class).isEmpty() || !scan(PlanningEntity.class).isEmpty();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Scanning for @%s and @%s annotations failed."
                    .formatted(PlanningSolution.class.getSimpleName(), PlanningEntity.class.getSimpleName()), e);
        }
    }

    public Class<?> findFirstSolutionClass() {
        Set<Class<?>> solutionClassSet;
        try {
            solutionClassSet = scan(PlanningSolution.class);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "Scanning for @%s annotations failed.".formatted(PlanningSolution.class.getSimpleName()), e);
        }
        if (solutionClassSet.isEmpty()) {
            return null;
        }
        return solutionClassSet.stream()
                .filter(TimefoldSolverAutoConfiguration.UNIQUENESS_PREDICATE)
                .findFirst()
                .orElse(null);
    }

    public List<Class<?>> findEntityClassList() {
        try {
            // Add all annotated classes
            var entityClassSet = new HashSet<>(scan(PlanningEntity.class));
            var childEntityList = new ArrayList<>(entityClassSet);
            // Now we search for child classes as well
            while (!childEntityList.isEmpty()) {
                var entityClass = childEntityList.remove(0);
                // Check all subclasses
                var childEntityClassList = findImplementingClassList(entityClass).stream()
                        .filter(c -> !c.equals(entityClass))
                        .toList();
                if (!childEntityClassList.isEmpty()) {
                    entityClassSet.addAll(childEntityClassList);
                    childEntityList.addAll(childEntityClassList);
                }
            }
            return new ArrayList<>(entityClassSet);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Scanning for @%s failed.".formatted(PlanningEntity.class.getSimpleName()), e);
        }
    }

    private Set<Class<?>> findAllClassesUsingClassLoader(ClassLoader classLoader, String packageName) throws IOException {
        try (InputStream stream = classLoader.getResourceAsStream(packageName.replaceAll("[.]", "/"));
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            return reader.lines()
                    .filter(line -> line.endsWith(".class"))
                    .map(className -> packageName + "." + className.substring(0, className.lastIndexOf('.')))
                    .map(className -> getClass(classLoader, className))
                    .collect(Collectors.toSet());
        }
    }

    private Class<?> getClass(ClassLoader classLoader, String className) {
        try {
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException e) {
            // ignore the exception
        }
        return null;
    }

    @Override
    protected ClassPathScanningCandidateComponentProvider
            createClassPathScanningCandidateComponentProvider(ApplicationContext context) {
        return new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                AnnotationMetadata metadata = beanDefinition.getMetadata();
                // Do not exclude abstract classes nor interfaces
                return metadata.isIndependent();
            }
        };
    }

}
