package ai.timefold.solver.test.api.solver.change;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import ai.timefold.solver.core.api.solver.change.ProblemChangeDirector;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Use for unit-testing {@link ai.timefold.solver.core.api.solver.change.ProblemChange}s.
 *
 * Together with Mockito this class makes it possible to verify that a
 * {@link ai.timefold.solver.core.api.solver.change.ProblemChange} implementation correctly calls methods of
 * the {@link ProblemChangeDirector}.
 *
 * Example of usage:
 *
 * <pre>
 * {@code java
 *  MockProblemChangeDirector mockProblemChangeDirector = spy(new MockProblemChangeDirector());
 *  ProblemChange problemChange = new MyProblemChange(removedEntity);
 *  problemChange.doChange(solution, mockProblemChangeDirector);
 *  verify(mockProblemChangeDirector).removeEntity(same(removedEntity), any());
 * }
 * </pre>
 */
public class MockProblemChangeDirector implements ProblemChangeDirector {

    private Map<Object, Object> lookUpTable = new IdentityHashMap<>();

    @Override
    public <Entity> void addEntity(@NonNull Entity entity, @NonNull Consumer<Entity> entityConsumer) {
        entityConsumer.accept(lookUpWorkingObjectOrFail(entity));
    }

    @Override
    public <Entity> void removeEntity(@NonNull Entity entity, Consumer<Entity> entityConsumer) {
        entityConsumer.accept(lookUpWorkingObjectOrFail(entity));
    }

    @Override
    public <Entity> void changeVariable(@NonNull Entity entity, @NonNull String variableName,
            @NonNull Consumer<Entity> entityConsumer) {
        entityConsumer.accept(lookUpWorkingObjectOrFail(entity));
    }

    @Override
    public <ProblemFact> void addProblemFact(@NonNull ProblemFact problemFact,
            @NonNull Consumer<ProblemFact> problemFactConsumer) {
        problemFactConsumer.accept(lookUpWorkingObjectOrFail(problemFact));
    }

    @Override
    public <ProblemFact> void removeProblemFact(@NonNull ProblemFact problemFact,
            @NonNull Consumer<ProblemFact> problemFactConsumer) {
        problemFactConsumer.accept(lookUpWorkingObjectOrFail(problemFact));
    }

    @Override
    public <EntityOrProblemFact> void changeProblemProperty(@NonNull EntityOrProblemFact problemFactOrEntity,
            @NonNull Consumer<EntityOrProblemFact> problemFactOrEntityConsumer) {
        problemFactOrEntityConsumer.accept(lookUpWorkingObjectOrFail(problemFactOrEntity));
    }

    /**
     * If the look-up result has been provided by a {@link #whenLookingUp(Object)} call, returns the defined object.
     * Otherwise, returns the original externalObject.
     *
     * @param externalObject entity or problem fact to look up
     */
    @Override
    public <EntityOrProblemFact> @Nullable EntityOrProblemFact
            lookUpWorkingObjectOrFail(@Nullable EntityOrProblemFact externalObject) {
        EntityOrProblemFact entityOrProblemFact = (EntityOrProblemFact) lookUpTable.get(externalObject);
        return entityOrProblemFact == null ? externalObject : entityOrProblemFact;
    }

    /**
     * If the look-up result has been provided by a {@link #whenLookingUp(Object)} call, returns the defined object.
     * Otherwise, returns null.
     *
     * @param externalObject entity or problem fact to look up
     */
    @Override
    public <EntityOrProblemFact> Optional<EntityOrProblemFact>
            lookUpWorkingObject(@Nullable EntityOrProblemFact externalObject) {
        return Optional.ofNullable((EntityOrProblemFact) lookUpTable.get(externalObject));
    }

    @Override
    public void updateShadowVariables() {
        // Do nothing.
    }

    /**
     * Defines what {@link #lookUpWorkingObjectOrFail(Object)} returns.
     */
    public @NonNull LookUpMockBuilder whenLookingUp(Object forObject) {
        return new LookUpMockBuilder(forObject);
    }

    public final class LookUpMockBuilder {
        private final Object forObject;

        public LookUpMockBuilder(Object forObject) {
            this.forObject = forObject;
        }

        public MockProblemChangeDirector thenReturn(Object returnObject) {
            lookUpTable.put(forObject, returnObject);
            return MockProblemChangeDirector.this;
        }
    }
}
