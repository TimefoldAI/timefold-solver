package ai.timefold.solver.core.impl.domain.variable;

import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface InnerListVariableListener<Solution_, Entity_, Element_>
        extends InnerVariableListener<Solution_, ListElementsChangeEvent<Entity_>> {
    void afterListElementUnassigned(InnerScoreDirector<Solution_, ?> scoreDirector, Element_ unassignedElement);
}
