package ai.timefold.solver.quarkus.deployment;

import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.SortedSet;

import ai.timefold.solver.core.impl.domain.solution.cloner.gizmo.GizmoSolutionCloner;
import ai.timefold.solver.core.impl.domain.solution.cloner.gizmo.GizmoSolutionClonerImplementor;
import ai.timefold.solver.core.impl.domain.solution.cloner.gizmo.GizmoSolutionOrEntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

class QuarkusGizmoSolutionClonerImplementor extends GizmoSolutionClonerImplementor {
    @Override
    protected void createFields(ClassCreator classCreator) {
        // do nothing, we don't need a shallow cloner
    }

    protected void createSetSolutionDescriptor(ClassCreator classCreator, SolutionDescriptor<?> solutionDescriptor) {
        // do nothing, we don't need to create a shallow cloner
        MethodCreator methodCreator = classCreator.getMethodCreator(
                MethodDescriptor.ofMethod(GizmoSolutionCloner.class, "setSolutionDescriptor", void.class,
                        SolutionDescriptor.class));

        methodCreator.returnValue(null);
    }

    @Override
    protected BytecodeCreator createUnknownClassHandler(BytecodeCreator bytecodeCreator,
            SolutionDescriptor<?> solutionDescriptor,
            Class<?> entityClass,
            ResultHandle toClone,
            ResultHandle cloneMap) {
        // do nothing, since we cannot encounter unknown classes
        return bytecodeCreator;
    }

    @Override
    protected void createAbstractDeepCloneHelperMethod(ClassCreator classCreator,
            Class<?> entityClass,
            SolutionDescriptor<?> solutionDescriptor,
            Map<Class<?>, GizmoSolutionOrEntityDescriptor> memoizedSolutionOrEntityDescriptorMap,
            SortedSet<Class<?>> deepClonedClassesSortedSet) {
        MethodCreator methodCreator =
                classCreator.getMethodCreator(getEntityHelperMethodName(entityClass), entityClass, entityClass, Map.class);
        methodCreator.setModifiers(Modifier.STATIC | Modifier.PRIVATE);

        GizmoSolutionOrEntityDescriptor entityDescriptor =
                memoizedSolutionOrEntityDescriptorMap.computeIfAbsent(entityClass,
                        (key) -> new GizmoSolutionOrEntityDescriptor(solutionDescriptor, entityClass));

        ResultHandle toClone = methodCreator.getMethodParam(0);
        ResultHandle cloneMap = methodCreator.getMethodParam(1);
        ResultHandle maybeClone = methodCreator.invokeInterfaceMethod(
                GET_METHOD, cloneMap, toClone);
        BranchResult hasCloneBranchResult = methodCreator.ifNotNull(maybeClone);
        BytecodeCreator hasCloneBranch = hasCloneBranchResult.trueBranch();
        hasCloneBranch.returnValue(maybeClone);

        BytecodeCreator noCloneBranch = hasCloneBranchResult.falseBranch();
        ResultHandle errorMessageBuilder = noCloneBranch.newInstance(MethodDescriptor.ofConstructor(StringBuilder.class));
        noCloneBranch.invokeVirtualMethod(
                MethodDescriptor.ofMethod(StringBuilder.class, "append", StringBuilder.class, String.class),
                errorMessageBuilder, noCloneBranch.load("Impossible state: encountered unknown subclass ("));
        noCloneBranch.invokeVirtualMethod(
                MethodDescriptor.ofMethod(StringBuilder.class, "append", StringBuilder.class, Object.class),
                errorMessageBuilder,
                noCloneBranch.invokeVirtualMethod(MethodDescriptor.ofMethod(Object.class, "getClass", Class.class),
                        toClone));
        noCloneBranch.invokeVirtualMethod(
                MethodDescriptor.ofMethod(StringBuilder.class, "append", StringBuilder.class, String.class),
                errorMessageBuilder, noCloneBranch.load(") of (" + entityClass + ") in Quarkus."));

        ResultHandle error =
                noCloneBranch.newInstance(MethodDescriptor.ofConstructor(IllegalStateException.class, String.class),
                        noCloneBranch.invokeVirtualMethod(
                                MethodDescriptor.ofMethod(StringBuilder.class, "toString", String.class),
                                errorMessageBuilder));
        noCloneBranch.throwException(error);
    }
}
