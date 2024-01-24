package ai.timefold.solver.spring.boot.autoconfigure;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.domain.EntityScanPackages;
import org.springframework.boot.autoconfigure.domain.EntityScanner;
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
        return Optional.ofNullable(findImplementingClassList(targetClass)).filter(classes -> !classes.isEmpty())
                .map(classes -> classes.get(0)).orElse(null);
    }

    public <T> List<Class<? extends T>> findImplementingClassList(Class<T> targetClass) {
        if (!AutoConfigurationPackages.has(context)) {
            return emptyList();
        }
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.setEnvironment(context.getEnvironment());
        scanner.setResourceLoader(context);
        scanner.addIncludeFilter(new AssignableTypeFilter(targetClass));

        EntityScanPackages entityScanPackages = EntityScanPackages.get(context);

        Set<String> packages = new HashSet<>();
        packages.addAll(AutoConfigurationPackages.get(context));
        packages.addAll(entityScanPackages.getPackageNames());
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
                        throw new IllegalStateException("The " + targetClass.getSimpleName() + " class ("
                                + candidate.getBeanClassName() + ") cannot be found.", e);
                    }
                })
                .collect(Collectors.toList());
    }

    public boolean hasSolutionOrEntityClasses() {
        try {
            return !scan(PlanningSolution.class).isEmpty() || !scan(PlanningEntity.class).isEmpty();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Scanning for @" + PlanningSolution.class.getSimpleName()
                    + " and @" + PlanningEntity.class.getSimpleName() + " annotations failed.", e);
        }
    }

    public Class<?> findFirstSolutionClass() {
        Set<Class<?>> solutionClassSet;
        try {
            solutionClassSet = scan(PlanningSolution.class);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Scanning for @" + PlanningSolution.class.getSimpleName()
                    + " annotations failed.", e);
        }
        return solutionClassSet.iterator().next();
    }

    public List<Class<?>> findEntityClassList() {
        Set<Class<?>> entityClassSet;
        try {
            entityClassSet = scan(PlanningEntity.class);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Scanning for @" + PlanningEntity.class.getSimpleName() + " failed.", e);
        }
        return new ArrayList<>(entityClassSet);
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
