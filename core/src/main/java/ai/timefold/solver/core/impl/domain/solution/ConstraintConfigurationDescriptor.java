package ai.timefold.solver.core.impl.domain.solution;

import static ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory.MemberAccessorType.FIELD_OR_READ_METHOD;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.domain.common.DomainAccessType;
import ai.timefold.solver.core.api.domain.constraintweight.ConstraintConfiguration;
import ai.timefold.solver.core.api.domain.constraintweight.ConstraintConfigurationProvider;
import ai.timefold.solver.core.api.domain.constraintweight.ConstraintWeight;
import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.common.ReflectionHelper;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @deprecated Use {@link ConstraintWeightOverrides} instead.
 */
@Deprecated(forRemoval = true, since = "1.13.0")
final class ConstraintConfigurationDescriptor<Solution_> {

    private final SolutionDescriptor<Solution_> solutionDescriptor;

    private final Class<?> constraintConfigurationClass;
    private String constraintPackage;

    private final Map<String, ConstraintWeightDescriptor<Solution_>> constraintWeightDescriptorMap;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public ConstraintConfigurationDescriptor(SolutionDescriptor<Solution_> solutionDescriptor,
            Class<?> constraintConfigurationClass) {
        this.solutionDescriptor = solutionDescriptor;
        this.constraintConfigurationClass = constraintConfigurationClass;
        constraintWeightDescriptorMap = new LinkedHashMap<>();
    }

    public String getConstraintPackage() {
        return constraintPackage;
    }

    public ConstraintWeightDescriptor<Solution_> getConstraintWeightDescriptor(String propertyName) {
        return constraintWeightDescriptorMap.get(propertyName);
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    public void processAnnotations(MemberAccessorFactory memberAccessorFactory, DomainAccessType domainAccessType,
            ScoreDefinition<?> scoreDefinition) {
        processPackAnnotation();
        var potentiallyOverwritingMethodList = new ArrayList<Method>();
        // Iterate inherited members too (unlike for EntityDescriptor where each one is declared)
        // to make sure each one is registered
        for (var lineageClass : ConfigUtils.getAllAnnotatedLineageClasses(constraintConfigurationClass,
                ConstraintConfiguration.class)) {
            var memberList = ConfigUtils.getDeclaredMembers(lineageClass);
            for (var member : memberList) {
                if (member instanceof Method method && potentiallyOverwritingMethodList.stream().anyMatch(
                        m -> member.getName().equals(m.getName()) // Shortcut to discard negatives faster
                                && ReflectionHelper.isMethodOverwritten(method, m.getDeclaringClass()))) {
                    // Ignore member because it is an overwritten method
                    continue;
                }
                processParameterAnnotation(memberAccessorFactory, domainAccessType, member, scoreDefinition);
            }
            potentiallyOverwritingMethodList.ensureCapacity(potentiallyOverwritingMethodList.size() + memberList.size());
            memberList.stream()
                    .filter(Method.class::isInstance)
                    .forEach(member -> potentiallyOverwritingMethodList.add((Method) member));
        }
        if (constraintWeightDescriptorMap.isEmpty()) {
            throw new IllegalStateException("The constraintConfigurationClass (" + constraintConfigurationClass
                    + ") must have at least 1 member with a "
                    + ConstraintWeight.class.getSimpleName() + " annotation.");
        }
    }

    private void processPackAnnotation() {
        var packAnnotation = constraintConfigurationClass.getAnnotation(ConstraintConfiguration.class);
        if (packAnnotation == null) {
            throw new IllegalStateException("The constraintConfigurationClass (" + constraintConfigurationClass
                    + ") has been specified as a @" + ConstraintConfigurationProvider.class.getSimpleName()
                    + " in the solution class (" + solutionDescriptor.getSolutionClass() + ")," +
                    " but does not have a @" + ConstraintConfiguration.class.getSimpleName() + " annotation.");
        }
        // If a @ConstraintConfiguration extends a @ConstraintConfiguration, their constraintPackage might differ.
        // So the ConstraintWeightDescriptors parse packAnnotation.constraintPackage() themselves.
        constraintPackage = packAnnotation.constraintPackage();
        if (constraintPackage.isEmpty()) {
            var pack = constraintConfigurationClass.getPackage();
            constraintPackage = (pack == null) ? "" : pack.getName();
        }
    }

    private void processParameterAnnotation(MemberAccessorFactory memberAccessorFactory, DomainAccessType domainAccessType,
            Member member, ScoreDefinition<?> scoreDefinition) {
        if (((AnnotatedElement) member).isAnnotationPresent(ConstraintWeight.class)) {
            var memberAccessor = memberAccessorFactory.buildAndCacheMemberAccessor(member, FIELD_OR_READ_METHOD,
                    ConstraintWeight.class, domainAccessType);
            if (constraintWeightDescriptorMap.containsKey(memberAccessor.getName())) {
                var duplicate = constraintWeightDescriptorMap.get(memberAccessor.getName()).getMemberAccessor();
                throw new IllegalStateException("The constraintConfigurationClass (" + constraintConfigurationClass
                        + ") has a @" + ConstraintWeight.class.getSimpleName()
                        + " annotated member (" + memberAccessor
                        + ") that is duplicated by a member (" + duplicate + ").\n"
                        + "Maybe the annotation is defined on both the field and its getter.");
            }
            if (!scoreDefinition.getScoreClass().isAssignableFrom(memberAccessor.getType())) {
                throw new IllegalStateException("The constraintConfigurationClass (" + constraintConfigurationClass
                        + ") has a @" + ConstraintWeight.class.getSimpleName()
                        + " annotated member (" + memberAccessor
                        + ") with a return type (" + memberAccessor.getType()
                        + ") that is not assignable to the score class (" + scoreDefinition.getScoreClass() + ").\n"
                        + "Maybe make that member (" + memberAccessor.getName() + ") return the score class ("
                        + scoreDefinition.getScoreClass().getSimpleName() + ") instead.");
            }
            var constraintWeightDescriptor = new ConstraintWeightDescriptor<Solution_>(memberAccessor);
            constraintWeightDescriptorMap.put(memberAccessor.getName(), constraintWeightDescriptor);
        }
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    public SolutionDescriptor<Solution_> getSolutionDescriptor() {
        return solutionDescriptor;
    }

    public Class<?> getConstraintConfigurationClass() {
        return constraintConfigurationClass;
    }

    public Set<ConstraintRef> getSupportedConstraints() {
        return constraintWeightDescriptorMap.values()
                .stream()
                .map(ConstraintWeightDescriptor::getConstraintRef)
                .collect(Collectors.toSet());
    }

    public ConstraintWeightDescriptor<Solution_> findConstraintWeightDescriptor(ConstraintRef constraintRef) {
        return constraintWeightDescriptorMap.values().stream()
                .filter(constraintWeightDescriptor -> constraintWeightDescriptor.getConstraintRef().equals(constraintRef))
                .findFirst()
                .orElse(null);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + constraintConfigurationClass.getName() + ")";
    }
}
