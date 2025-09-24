package ai.timefold.solver.core.impl.domain.variable;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface InnerBasicVariableListener<Solution_, Entity_>
        extends InnerVariableListener<Solution_, BasicVariableChangeEvent<Entity_>> {
}
