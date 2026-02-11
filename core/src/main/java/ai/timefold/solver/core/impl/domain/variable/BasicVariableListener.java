package ai.timefold.solver.core.impl.domain.variable;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface BasicVariableListener<Solution_, Entity_>
        extends VariableListener<Solution_, BasicVariableChangeEvent<Entity_>> {
}
