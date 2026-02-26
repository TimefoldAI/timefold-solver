package ai.timefold.solver.core.api.solver.change;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Use for unit-testing {@link ProblemChange}s.
 * Together with Mockito this class makes it possible to verify that a
 * {@link ProblemChange} implementation correctly calls methods of
 * the {@link ProblemChangeDirector}.
 * <p>
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
@NullMarked
public class MockProblemChangeDirector implements ProblemChangeDirector {

    private Map<Object, Object> lookUpTable = new IdentityHashMap<>();

    @Override
    public <Entity> void addEntity(Entity entity, Consumer<Entity> entityConsumer) {
        entityConsumer.accept(this.lookUpWorkingObject(entity));
    }

    @Override
    public <Entity> void removeEntity(Entity entity, Consumer<Entity> entityConsumer) {
        entityConsumer.accept(this.lookUpWorkingObject(entity));
    }

    @Override
    public <Entity> void changeVariable(Entity entity, String variableName, Consumer<Entity> entityConsumer) {
        entityConsumer.accept(this.lookUpWorkingObject(entity));
    }

    @Override
    public <ProblemFact> void addProblemFact(ProblemFact problemFact, Consumer<ProblemFact> problemFactConsumer) {
        problemFactConsumer.accept(this.lookUpWorkingObject(problemFact));
    }

    @Override
    public <ProblemFact> void removeProblemFact(ProblemFact problemFact, Consumer<ProblemFact> problemFactConsumer) {
        problemFactConsumer.accept(this.lookUpWorkingObject(problemFact));
    }

    @Override
    public <EntityOrProblemFact> void changeProblemProperty(EntityOrProblemFact problemFactOrEntity,
            Consumer<EntityOrProblemFact> problemFactOrEntityConsumer) {
        problemFactOrEntityConsumer.accept(this.lookUpWorkingObject(problemFactOrEntity));
    }

    /**
     * If the look-up result has been provided by a {@link #whenLookingUp(Object)} call, returns the defined object.
     * Otherwise, returns the original externalObject.
     *
     * @param externalObject entity or problem fact to look up
     */
    @Override
    public <EntityOrProblemFact> @Nullable EntityOrProblemFact
            lookUpWorkingObject(@Nullable EntityOrProblemFact externalObject) {
        EntityOrProblemFact entityOrProblemFact = (EntityOrProblemFact) lookUpTable.get(externalObject);
        return entityOrProblemFact == null ? externalObject : entityOrProblemFact;
    }

    @Override
    public void updateShadowVariables() {
        // Do nothing.
    }

    /**
     * Defines what {@link #lookUpWorkingObject(Object)} returns.
     */
    public LookUpMockBuilder whenLookingUp(Object forObject) {
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
