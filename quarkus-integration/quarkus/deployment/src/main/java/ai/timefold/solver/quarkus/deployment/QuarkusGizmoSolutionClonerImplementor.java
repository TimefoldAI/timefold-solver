package ai.timefold.solver.quarkus.deployment;

import java.lang.reflect.Modifier;
import java.util.Map;

import ai.timefold.solver.core.impl.domain.solution.cloner.gizmo.GizmoSolutionCloner;
import ai.timefold.solver.core.impl.domain.solution.cloner.gizmo.GizmoSolutionClonerImplementor;
import ai.timefold.solver.core.impl.domain.solution.cloner.gizmo.GizmoSolutionOrEntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

class QuarkusGizmoSolutionClonerImplementor extends GizmoSolutionClonerImplementor {
    @Override
    protected void createFields(ClonerDescriptor clonerDescriptor) {
        // do nothing, we don't need a shallow cloner
    }

    @Override
    protected void createSetSolutionDescriptor(ClonerDescriptor clonerDescriptor) {
        // do nothing, we don't need to create a shallow cloner
        MethodCreator methodCreator = clonerDescriptor.classCreator().getMethodCreator(
                MethodDescriptor.ofMethod(GizmoSolutionCloner.class, "setSolutionDescriptor", void.class,
                        SolutionDescriptor.class));

        methodCreator.returnValue(null);
    }

    @Override
    protected BytecodeCreator createUnknownClassHandler(ClonerDescriptor clonerDescriptor,
            ClonerMethodDescriptor clonerMethodDescriptor,
            Class<?> entityClass,
            ResultHandle toClone) {
        // do nothing, since we cannot encounter unknown classes
        return clonerMethodDescriptor.bytecodeCreator();
    }

    @Override
    protected void createAbstractDeepCloneHelperMethod(ClonerDescriptor clonerDescriptor,
            Class<?> entityClass) {
        MethodCreator methodCreator =
                clonerDescriptor.classCreator().getMethodCreator(getEntityHelperMethodName(entityClass), entityClass,
                        entityClass, Map.class);
        methodCreator.setModifiers(Modifier.STATIC | Modifier.PRIVATE);

        clonerDescriptor.memoizedSolutionOrEntityDescriptorMap().computeIfAbsent(entityClass,
                key -> new GizmoSolutionOrEntityDescriptor(clonerDescriptor.solutionDescriptor(), entityClass));

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
