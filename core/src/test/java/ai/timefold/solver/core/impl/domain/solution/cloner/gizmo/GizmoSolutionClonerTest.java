package ai.timefold.solver.core.impl.domain.solution.cloner.gizmo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.domain.solution.cloner.SolutionCloner;
import ai.timefold.solver.core.impl.domain.common.ReflectionHelper;
import ai.timefold.solver.core.impl.domain.common.accessor.gizmo.GizmoClassLoader;
import ai.timefold.solver.core.impl.domain.common.accessor.gizmo.GizmoMemberDescriptor;
import ai.timefold.solver.core.impl.domain.solution.cloner.AbstractSolutionClonerTest;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childnot.TestdataOnlyBaseAnnotatedChildEntity;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childnot.TestdataOnlyBaseAnnotatedExtendedSolution;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childnot.TestdataOnlyBaseAnnotatedSolution;

import org.junit.jupiter.api.Test;

import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;

class GizmoSolutionClonerTest extends AbstractSolutionClonerTest {

    @Test
    void debuggingDisabled() {
        assertThat(GizmoSolutionClonerImplementor.DEBUG)
                .as("Gizmo debugging is enabled. Please disable before merging changes.")
                .isFalse();
    }

    @Override
    protected <Solution_> SolutionCloner<Solution_> createSolutionCloner(SolutionDescriptor<Solution_> solutionDescriptor) {
        var className = GizmoSolutionClonerFactory.getGeneratedClassName(solutionDescriptor);
        var classBytecodeHolder = new HashMap<String, byte[]>();
        var classOutput =
                GizmoSolutionClonerImplementor.createClassOutputWithDebuggingCapability(classBytecodeHolder);

        if (GizmoSolutionClonerImplementor.DEBUG) {
            System.setProperty("gizmo.debug", "true");
        }
        var gizmo = Gizmo.create(classOutput);

        gizmo.class_(className, classCreator -> {
            classCreator.implements_(GizmoSolutionCloner.class);
            classCreator.extends_(Object.class);
            classCreator.final_();

            var memoizedSolutionOrEntityDescriptorMap = new HashMap<Class<?>, GizmoSolutionOrEntityDescriptor>();

            var deepClonedClassSet = GizmoCloningUtils.getDeepClonedClasses(solutionDescriptor, Collections.emptyList());
            Stream.concat(Stream.of(solutionDescriptor.getSolutionClass()),
                    Stream.concat(solutionDescriptor.getEntityClassSet().stream(),
                            deepClonedClassSet.stream()))
                    .forEach(clazz -> {
                        memoizedSolutionOrEntityDescriptorMap.put(clazz,
                                generateGizmoSolutionOrEntityDescriptor(solutionDescriptor, clazz));
                    });

            GizmoSolutionClonerImplementor.defineClonerFor(classCreator, solutionDescriptor,
                    Collections.singleton(solutionDescriptor.getSolutionClass()),
                    memoizedSolutionOrEntityDescriptorMap, deepClonedClassSet);
        });

        var gizmoClassLoader = new GizmoClassLoader(classBytecodeHolder);

        try {
            @SuppressWarnings("unchecked")
            var solutionCloner =
                    (GizmoSolutionCloner<Solution_>) gizmoClassLoader.loadClass(className).getConstructor().newInstance();
            solutionCloner.setSolutionDescriptor(solutionDescriptor);
            return solutionCloner;
        } catch (Exception e) {
            throw new IllegalStateException("Failed creating generated Gizmo Class (" + className + ").", e);
        }
    }

    // HACK: use public getters/setters of fields so test domain can remain private
    // TODO: should this be another DomainAccessType? DomainAccessType.GIZMO_RELAXED_ACCESS?
    private GizmoSolutionOrEntityDescriptor generateGizmoSolutionOrEntityDescriptor(SolutionDescriptor solutionDescriptor,
            Class<?> entityClass) {
        var solutionFieldToMemberDescriptor = new HashMap<Field, GizmoMemberDescriptor>();
        var currentClass = entityClass;

        while (currentClass != null) {
            for (var field : currentClass.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    GizmoMemberDescriptor member;
                    var declaringClass = field.getDeclaringClass();
                    var memberDescriptor = FieldDesc.of(field);
                    var name = field.getName();

                    if (Modifier.isPublic(field.getModifiers())) {
                        member = new GizmoMemberDescriptor(name, memberDescriptor, declaringClass);
                    } else {
                        var getter = ReflectionHelper.getGetterMethod(currentClass, field.getName());
                        var setter = ReflectionHelper.getSetterMethod(currentClass, field.getName());
                        if (getter != null && setter != null) {
                            var getterDescriptor = MethodDesc.of(field.getDeclaringClass(),
                                    getter.getName(),
                                    field.getType());
                            var setterDescriptor = MethodDesc.of(field.getDeclaringClass(),
                                    setter.getName(),
                                    setter.getReturnType(),
                                    field.getType());
                            member = new GizmoMemberDescriptor(name, getterDescriptor, memberDescriptor, declaringClass,
                                    setterDescriptor);
                        } else {
                            throw new IllegalStateException("""
                                    Failed to generate GizmoMemberDescriptor for (%s#%s):
                                    Field is not public and does not have both a getter and a setter.
                                    """.formatted(declaringClass.getName(), name));
                        }
                    }
                    solutionFieldToMemberDescriptor.put(field, member);
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        return new GizmoSolutionOrEntityDescriptor(solutionDescriptor, entityClass, solutionFieldToMemberDescriptor);
    }

    private interface Animal {
    }

    private interface Robot {
    }

    private interface Zebra extends Animal {
    }

    private interface RobotZebra extends Zebra, Robot {
    }

    // This test verifies the instanceof comparator works correctly
    @Test
    void instanceOfComparatorTest() {
        var classSet = new HashSet<>(Arrays.asList(
                Animal.class,
                Robot.class,
                Zebra.class,
                RobotZebra.class));

        var comparator = GizmoSolutionClonerImplementor.getInstanceOfComparator(classSet);

        // assert that the comparator works on equality
        assertThat(comparator.compare(Animal.class, Animal.class)).isZero();
        assertThat(comparator.compare(Robot.class, Robot.class)).isZero();
        assertThat(comparator.compare(Zebra.class, Zebra.class)).isZero();
        assertThat(comparator.compare(RobotZebra.class, RobotZebra.class)).isZero();

        // Zebra < Animal and Robot
        // Since Animal and Robot are base classes (i.e. not subclasses of anything in the set)
        // and Zebra is a subclass of Animal
        assertThat(comparator.compare(Zebra.class, Animal.class)).isLessThan(0);
        assertThat(comparator.compare(Zebra.class, Robot.class)).isLessThan(0);
        assertThat(comparator.compare(Animal.class, Zebra.class)).isGreaterThan(0);
        assertThat(comparator.compare(Robot.class, Zebra.class)).isGreaterThan(0);

        // RobotZebra < Animal and Robot and Zebra
        assertThat(comparator.compare(RobotZebra.class, Animal.class)).isLessThan(0);
        assertThat(comparator.compare(RobotZebra.class, Robot.class)).isLessThan(0);
        assertThat(comparator.compare(RobotZebra.class, Zebra.class)).isLessThan(0);
        assertThat(comparator.compare(Animal.class, RobotZebra.class)).isGreaterThan(0);
        assertThat(comparator.compare(Robot.class, RobotZebra.class)).isGreaterThan(0);
        assertThat(comparator.compare(Zebra.class, RobotZebra.class)).isGreaterThan(0);
    }

    // This test verifies a proper error message is thrown if an extended solution is passed.
    @Override
    @Test
    protected void cloneExtendedSolution() {
        var solutionDescriptor = TestdataOnlyBaseAnnotatedExtendedSolution.buildBaseSolutionDescriptor();
        var cloner = createSolutionCloner(solutionDescriptor);

        var val1 = new TestdataValue("1");
        var val2 = new TestdataValue("2");
        var val3 = new TestdataValue("3");
        var a = new TestdataOnlyBaseAnnotatedChildEntity("a", val1, null);
        var b = new TestdataOnlyBaseAnnotatedChildEntity("b", val1, "extraObjectOnEntity");
        var c = new TestdataOnlyBaseAnnotatedChildEntity("c", val3);
        var d = new TestdataOnlyBaseAnnotatedChildEntity("d", val3, c);
        c.setExtraObject(d);

        var original = new TestdataOnlyBaseAnnotatedExtendedSolution("solution", "extraObjectOnSolution");
        var valueList = Arrays.asList(val1, val2, val3);
        original.setValueList(valueList);
        var originalEntityList = Arrays.asList(a, b, c, d);
        original.setEntityList(originalEntityList);

        assertThatCode(() -> cloner.cloneSolution(original))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContainingAll(
                        "Failed to create clone: encountered (%s)".formatted(original.getClass()),
                        "which is not a known subclass of the solution class (%s)."
                                .formatted(TestdataOnlyBaseAnnotatedSolution.class),
                        "The known subclasses are: [%s]".formatted(TestdataOnlyBaseAnnotatedSolution.class.getName()),
                        "Maybe use DomainAccessType.REFLECTION?");
    }
}
