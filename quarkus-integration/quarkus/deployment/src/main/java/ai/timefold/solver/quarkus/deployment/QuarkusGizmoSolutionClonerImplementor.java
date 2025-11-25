package ai.timefold.solver.quarkus.deployment;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.function.Consumer;

import ai.timefold.solver.core.impl.domain.solution.cloner.gizmo.GizmoSolutionClonerImplementor;
import ai.timefold.solver.core.impl.domain.solution.cloner.gizmo.GizmoSolutionOrEntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Var;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.MethodDesc;

class QuarkusGizmoSolutionClonerImplementor extends GizmoSolutionClonerImplementor {
    @Override
    protected ClonerDescriptor withFallbackClonerField(ClonerDescriptor clonerDescriptor) {
        // do nothing, we don't need a shallow cloner
        return clonerDescriptor;
    }

    @Override
    protected void createSetSolutionDescriptor(ClonerDescriptor clonerDescriptor) {
        // do nothing, we don't need to create a shallow cloner
        clonerDescriptor.classCreator().method("setSolutionDescriptor", methodMetadataCreator -> {
            methodMetadataCreator.returning(void.class);
            methodMetadataCreator.parameter("solutionDescriptor", SolutionDescriptor.class);
            methodMetadataCreator.body(BlockCreator::return_);
        });
    }

    @Override
    protected void handleUnknownClass(ClonerDescriptor clonerDescriptor,
            ClonerMethodDescriptor clonerMethodDescriptor,
            Class<?> entityClass,
            Var toClone,
            Consumer<BlockCreator> blockCreatorConsumer) {
        // do nothing, since we cannot encounter unknown classes
        blockCreatorConsumer.accept(clonerMethodDescriptor.blockCreator());
    }

    @Override
    protected void createAbstractDeepCloneHelperMethod(ClonerDescriptor clonerDescriptor,
            Class<?> entityClass) {
        clonerDescriptor.classCreator().staticMethod(getEntityHelperMethodName(entityClass), methodCreator -> {
            var toClone = methodCreator.parameter("toClone", entityClass);
            var cloneMap = methodCreator.parameter("cloneMap", Map.class);
            var ignoredIsBottom = methodCreator.parameter("ignoredIsBottom", boolean.class);
            var ignoredQueue = methodCreator.parameter("ignoredQueue", ArrayDeque.class);

            methodCreator.public_();
            methodCreator.returning(entityClass);
            methodCreator.body(blockCreator -> {
                clonerDescriptor.memoizedSolutionOrEntityDescriptorMap().computeIfAbsent(entityClass,
                        key -> new GizmoSolutionOrEntityDescriptor(clonerDescriptor.solutionDescriptor(), entityClass));

                var maybeClone = blockCreator.localVar("existingClone", blockCreator.withMap(cloneMap).get(toClone));
                blockCreator.ifNotNull(maybeClone, hasCloneBranch -> hasCloneBranch.return_(maybeClone));

                var errorMessageBuilder = blockCreator.localVar("messageBuilder", blockCreator.new_(StringBuilder.class));
                blockCreator.invokeVirtual(
                        MethodDesc.of(StringBuilder.class, "append", StringBuilder.class, String.class),
                        errorMessageBuilder, Const.of("Impossible state: encountered unknown subclass ("));
                blockCreator.invokeVirtual(
                        MethodDesc.of(StringBuilder.class, "append", StringBuilder.class, Object.class),
                        errorMessageBuilder,
                        blockCreator.withObject(toClone).getClass_());
                blockCreator.invokeVirtual(
                        MethodDesc.of(StringBuilder.class, "append", StringBuilder.class, String.class),
                        errorMessageBuilder, Const.of(") of (" + entityClass + ") in Quarkus."));

                var error = blockCreator.new_(ConstructorDesc.of(IllegalStateException.class, String.class),
                        blockCreator.withObject(errorMessageBuilder).toString_());
                blockCreator.throw_(error);
            });
        });
    }
}
