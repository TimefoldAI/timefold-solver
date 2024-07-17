from _jpyinterpreter import add_java_interface
from typing import TYPE_CHECKING
from abc import ABC, abstractmethod


@add_java_interface('ai.timefold.solver.core.api.score.calculator.IncrementalScoreCalculator')
class IncrementalScoreCalculator(ABC):
    """
    Used for incremental Python `Score` calculation.
    This is much faster than `easy_score_calculator` but requires much more code to implement too.

    Any implementation is naturally stateful.
    """
    @abstractmethod
    def after_entity_added(self, entity) -> None:
        ...

    @abstractmethod
    def after_entity_removed(self, entity) -> None:
        ...

    def after_list_variable_changed(self, entity, variable_name: str, start: int, end: int) -> None:
        ...

    def after_list_variable_element_assigned(self, variable_name: str, element) -> None:
        ...

    def after_list_variable_element_unassigned(self, variable_name: str, element) -> None:
        ...

    @abstractmethod
    def after_variable_changed(self, entity, variable_name: str) -> None:
        ...

    @abstractmethod
    def before_entity_added(self, entity) -> None:
        ...

    @abstractmethod
    def before_entity_removed(self, entity) -> None:
        ...

    def before_list_variable_changed(self, entity, variable_name: str, start: int, end: int) -> None:
        ...

    def before_list_variable_element_assigned(self, variable_name: str, element) -> None:
        ...

    def before_list_variable_element_unassigned(self, variable_name: str, element) -> None:
        ...

    @abstractmethod
    def before_variable_changed(self, entity, variable_name: str) -> None:
        ...

    @abstractmethod
    def calculate_score(self):
        """
        Notes
        -----
        This method is only called if the `Score` cannot be predicted.
        The `Score` can be predicted for example after an undo move.
        """
        ...

    @abstractmethod
    def reset_working_solution(self, solution) -> None:
        """
        Notes
        -----
        There are no `before_entity_added` and `after_entity_added`
        calls for entities that are already present in the working solution.
        """
        ...


@add_java_interface('ai.timefold.solver.core.api.score.calculator.ConstraintMatchAwareIncrementalScoreCalculator')
class ConstraintMatchAwareIncrementalScoreCalculator(IncrementalScoreCalculator):
    """
    Allows an `IncrementalScoreCalculator`
    to report `ConstraintMatchTotal` for explaining a score (= which score constraints match for how much)
    and also for score corruption analysis.
    """
    @abstractmethod
    def get_constraint_match_totals(self) -> list:
        """
        Notes
        -----
        If a constraint is present in the problem but resulted in no matches,
        it should still be present with a `ConstraintMatchTotal.constraint_match_set` size of 0.
        """
        ...

    @abstractmethod
    def get_indictment_map(self) -> dict | None:
        """
        Returns ``None`` if it should to be calculated non-incrementally from `get_constraint_match_totals`.
        """
        ...

    @abstractmethod
    def reset_working_solution(self, solution, constraint_match_enabled=False) -> None:
        """
        Allows for increased performance because it only tracks if `constraint_match_enabled` is true.
        """
        ...


if not TYPE_CHECKING:
    # Use type(self).method(self, ...)
    # Since if the arguments are typed, they might have different signatures and
    # thus not override one another, meaning the generated interface method will
    # call the original method and not the "overridden" one!
    def afterEntityAdded(self, entity) -> None:
        type(self).after_entity_added(self, entity)

    IncrementalScoreCalculator.afterEntityAdded = afterEntityAdded

    def afterEntityRemoved(self, entity) -> None:
        type(self).after_entity_removed(self, entity)

    IncrementalScoreCalculator.afterEntityRemoved = afterEntityRemoved

    def afterListVariableChanged(self, entity, variable_name, start, end) -> None:
        type(self).after_list_variable_changed(self, entity, variable_name, start, end)

    IncrementalScoreCalculator.afterListVariableChanged = afterListVariableChanged

    def afterListVariableElementAssigned(self, variable_name, element) -> None:
        type(self).after_list_variable_element_assigned(self, variable_name, element)

    IncrementalScoreCalculator.afterListVariableElementAssigned = afterListVariableElementAssigned

    def afterListVariableElementUnassigned(self, variable_name, element) -> None:
        type(self).after_list_variable_element_unassigned(self, variable_name, element)

    IncrementalScoreCalculator.afterListVariableElementUnassigned = afterListVariableElementUnassigned

    def afterVariableChanged(self, entity, variable_name) -> None:
        type(self).after_variable_changed(self, entity, variable_name)

    IncrementalScoreCalculator.afterVariableChanged = afterVariableChanged

    def beforeEntityAdded(self, entity) -> None:
        type(self).before_entity_added(self, entity)

    IncrementalScoreCalculator.beforeEntityAdded = beforeEntityAdded

    def beforeEntityRemoved(self, entity) -> None:
        type(self).before_entity_removed(self, entity)

    IncrementalScoreCalculator.beforeEntityRemoved = beforeEntityRemoved

    def beforeListVariableChanged(self, entity, variable_name, start, end) -> None:
        type(self).before_list_variable_changed(self, entity, variable_name, start, end)

    IncrementalScoreCalculator.beforeListVariableChanged = beforeListVariableChanged

    def beforeListVariableElementAssigned(self, variable_name, element) -> None:
        type(self).before_list_variable_element_assigned(self, variable_name, element)

    IncrementalScoreCalculator.beforeListVariableElementAssigned = beforeListVariableElementAssigned

    def beforeListVariableElementUnassigned(self, variable_name, element) -> None:
        type(self).before_list_variable_element_unassigned(self, variable_name, element)

    IncrementalScoreCalculator.beforeListVariableElementUnassigned = beforeListVariableElementUnassigned

    def beforeVariableChanged(self, entity, variable_name) -> None:
        type(self).before_variable_changed(self, entity, variable_name)

    IncrementalScoreCalculator.beforeVariableChanged = beforeVariableChanged

    def calculateScore(self):
        return type(self).calculate_score(self)._to_java_score()

    IncrementalScoreCalculator.calculateScore = calculateScore

    def resetWorkingSolution(self, solution) -> None:
        type(self).reset_working_solution(self, solution)

    IncrementalScoreCalculator.resetWorkingSolution = resetWorkingSolution

    def getConstraintMatchTotals(self) -> list:
        return type(self).get_constraint_match_totals(self)

    ConstraintMatchAwareIncrementalScoreCalculator.getConstraintMatchTotals = getConstraintMatchTotals

    def getIndictmentMap(self) -> dict:
        return type(self).get_indictment_map(self)

    ConstraintMatchAwareIncrementalScoreCalculator.getIndictmentMap = getIndictmentMap

    def resetWorkingSolution(self, solution, constraint_match_enabled=False) -> None:
        type(self).reset_working_solution(self, solution, constraint_match_enabled)

    ConstraintMatchAwareIncrementalScoreCalculator.resetWorkingSolution = resetWorkingSolution

__all__ = ['IncrementalScoreCalculator', 'ConstraintMatchAwareIncrementalScoreCalculator']
