from _jpyinterpreter import add_java_interface
from typing import TYPE_CHECKING, TypeVar

from ..score import ScoreDirector

if TYPE_CHECKING:
    pass

Solution_ = TypeVar('Solution_')
Entity_ = TypeVar('Entity_')


class VariableListenerMeta(type):
    def __new__(cls, clsname, bases, attrs):
        from .._timefold_java_interop import _add_to_compilation_queue
        out = super().__new__(cls, clsname, bases, attrs)
        _add_to_compilation_queue(out)
        return out


@add_java_interface('ai.timefold.solver.core.api.domain.variable.VariableListener')
class VariableListener(metaclass=VariableListenerMeta):
    """
    A listener sourced on a basic PlanningVariable.

    Changes shadow variables when a source basic planning variable changes.
    The source variable can be either a genuine or a shadow variable.

    Important: it must only change the shadow variable(s) for which it's configured!
    It should never change a genuine variable or a problem fact.
    It can change its shadow variable(s) on multiple entity instances
    (for example: an arrival_time change affects all trailing entities too).

    It is recommended to keep implementations stateless.
    If state must be implemented,
    implementations may need to override the methods `reset_working_solution`, and `close`.
    """
    def after_entity_added(self, score_director: ScoreDirector, entity) -> None:
        pass

    def after_entity_removed(self, score_director: ScoreDirector, entity) -> None:
        pass

    def before_entity_added(self, score_director: ScoreDirector, entity) -> None:
        pass

    def before_entity_removed(self, score_director: ScoreDirector, entity) -> None:
        pass

    def close(self) -> None:
        pass

    def reset_working_solution(self, score_director: ScoreDirector) -> None:
        pass

    def after_variable_changed(self, score_director: ScoreDirector, entity) -> None:
        pass

    def before_variable_changed(self, score_director: ScoreDirector, entity) -> None:
        pass

    def requires_unique_entity_events(self) -> bool:
        return False


if not TYPE_CHECKING:  # We do not want these methods to appear in the API
    def afterEntityAdded(self, java_score_director, entity) -> None:
        score_director = ScoreDirector(java_score_director)
        self.after_entity_added(score_director, entity)

    VariableListener.afterEntityAdded = afterEntityAdded

    def afterEntityRemoved(self, java_score_director, entity) -> None:
        score_director = ScoreDirector(java_score_director)
        self.after_entity_removed(score_director, entity)

    VariableListener.afterEntityRemoved = afterEntityRemoved

    def beforeEntityAdded(self, java_score_director, entity) -> None:
        score_director = ScoreDirector(java_score_director)
        self.before_entity_added(score_director, entity)

    VariableListener.beforeEntityAdded = beforeEntityAdded

    def beforeEntityRemoved(self, java_score_director, entity) -> None:
        score_director = ScoreDirector(java_score_director)
        self.before_entity_removed(score_director, entity)

    VariableListener.beforeEntityRemoved = beforeEntityRemoved

    def resetWorkingSolution(self, java_score_director) -> None:
        score_director = ScoreDirector(java_score_director)
        self.reset_working_solution(score_director)

    VariableListener.resetWorkingSolution = resetWorkingSolution

    def afterVariableChanged(self, java_score_director, entity) -> None:
        score_director = ScoreDirector(java_score_director)
        self.after_variable_changed(score_director, entity)

    VariableListener.afterVariableChanged = afterVariableChanged

    def beforeVariableChanged(self, java_score_director, entity) -> None:
        score_director = ScoreDirector(java_score_director)
        self.before_variable_changed(score_director, entity)

    VariableListener.beforeVariableChanged = beforeVariableChanged

    def requiresUniqueEntityEvents(self) -> bool:
        return self.requires_unique_entity_events()

    VariableListener.requiresUniqueEntityEvents = requiresUniqueEntityEvents


__all__ = ['VariableListener']
